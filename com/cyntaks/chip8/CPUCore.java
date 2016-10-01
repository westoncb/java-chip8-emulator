package com.cyntaks.chip8;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

public class CPUCore implements Runnable
{
  private Memory memory;
  private CPU cpu;
  private Screen screen;
  
  private boolean execute;
  private boolean debug = false;
  private boolean paused = false;
  
  private static final float CYCLE_TIME = .015f; //milliseconds
  private int pauseScale;
  
  private int[] keys;
  
  private AudioFormat format;
  private byte[] audio;
  private boolean looping;
  private boolean stopLoop;
  private boolean soundEnabled;
  
  private CHIP8 chip8;
  
  public CPUCore(CPU cpu, Memory memory, Screen screen, CHIP8 chip8)
  {
    this.chip8 = chip8;
    this.memory = memory;
    this.cpu = cpu;
    this.screen = screen;
    keys = new int[16];
    this.pauseScale = 150;
    
    audio = new byte[128];
    for (int i = 0; i < audio.length; i++) 
    {
      audio[i] = 0;
    }
    play();
    
    format = new AudioFormat(44100, 16, 1, true, false);
    loadSound("/sounds/beep.wav");
    soundEnabled = true;
    
    
  }
  
  public void reset()
  {
    looping = false;
    stopLoop = false;
    paused = false;
    endExecution();
    for (int i = 0; i < keys.length; i++) 
    {
      keys[i] = 0; 
    }
  }
  
  public void loadSound(String fileName)
    {
      AudioInputStream audioIn = null;      
      URL url = CPUCore.class.getResource(fileName);      
        try 
        {
          audioIn = AudioSystem.getAudioInputStream(url.openStream());
        } catch (IOException ex) {ex.printStackTrace();}
        catch (UnsupportedAudioFileException ex){ex.printStackTrace();}
        audio = new byte[(int)(audioIn.getFrameLength() * format.getFrameSize())];
        DataInputStream dataIn = new DataInputStream(audioIn);
        try {
          dataIn.readFully(audio);
        }catch(IOException ex){
          ex.printStackTrace();
        }
        audio[0] = 0;
    }
  
  public void loop()
  {
    looping = true;
    stopLoop = false;
    if(soundEnabled)
    {
      new Thread(){
        public void run()
        {
          int bufferSize = format.getFrameSize() * (int)(format.getSampleRate()/10);
          byte[] buffer = new byte[bufferSize];
          
          SourceDataLine line = null;
          try 
          {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(format, bufferSize);
          } catch (LineUnavailableException ex) 
          {
            ex.printStackTrace();
            return;
          }
          
          line.start();
          
          try 
          {
            int numBytesRead = 0;
            LoopingByteArrayInputStream bytesIn = new LoopingByteArrayInputStream(audio);
            while(numBytesRead != -1)
            {
              if(!looping)
                bytesIn.close();
              numBytesRead = bytesIn.read(buffer, 0, bufferSize);
              if(numBytesRead != -1)
                line.write(buffer, 0, numBytesRead);
              if(stopLoop)
                looping = false;
            }
          } catch (IOException ex) 
          {
            ex.printStackTrace();
          } 
          finally
          {
            line.drain();
            line.close();
          }
        }
      }.start();
    }
  }
  
  public void play()
  {
    if(soundEnabled)
    {
      new Thread(){
        public void run()
        {
          int bufferSize = format.getFrameSize() * (int)(format.getSampleRate()/10);
          byte[] buffer = new byte[bufferSize];
          
          SourceDataLine line = null;
          try 
          {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(format, bufferSize);
          } catch (LineUnavailableException ex) 
          {
            ex.printStackTrace();
            return;
          }
          
          line.start();
          
          int numBytesRead = 0;
          ByteArrayInputStream bytesIn = new ByteArrayInputStream(audio);
          while(numBytesRead != -1)
          {
            numBytesRead = bytesIn.read(buffer, 0, bufferSize);
            if(numBytesRead != -1)
              line.write(buffer, 0, numBytesRead);
          }
          
          line.drain();
          line.close();
        }
      }.start();
    }
  }
  
  public void keyPressed(char key)
  {
    keys[CHIP8.hexToInt(key)] = 1;
  }
  
  public void keyReleased(char key)
  {
    keys[CHIP8.hexToInt(key)] = 0;
  }
  
  private boolean isKeyPressed(int key)
  {
    boolean pressed = keys[key] == 1 ? true : false;
    return pressed;
  }
  
  private int getPressedKey()
  {
    for (int i = 0; i < keys.length; i++) 
    {
      if(keys[i] == 1) 
        return i;
    }
    
    return -1;
  }
    
  public void beginExecution()
  {
    new Thread(this).start();
  }
  
  public void endExecution()
  {
    if(execute)
    {
      execute = false;
      synchronized(this)
      {
        try 
        {
          this.wait(); 
        } catch (Exception ex) 
        {
          ex.printStackTrace();
        }
      }
    }
  }
  
  public void run()
  {
    execute = true;
    long start = Sys.getTime();
    long delta = 0;
    long loopCounter = 0;
    float cycleTime = 0;
    float thisCycle = 0;
    
    while(execute)
    {
      loopCounter++;
      start = Sys.getTime();
      
      if(!paused)
      {
        thisCycle = execute(memory.load(cpu.getProgramCounter()), //how much time the cycle really would have taken
                            memory.load(cpu.getProgramCounter()+1));
                                   
        if(cpu.getDelayTimer() > 0)
          cpu.setDelayTimer(cpu.getDelayTimer() - 1);
        if(cpu.getSoundTimer() > 0)
          cpu.setSoundTimer(cpu.getSoundTimer() - 1);
        else if(cpu.getSoundTimer() != -50000)
        { 
          cpu.setSoundTimer(-50000);
          stopLoop = true;
        }
        
        cpu.setProgramCounter(cpu.getProgramCounter() + 2); //go to the next line
        
        cycleTime += thisCycle;
      }
      else
      {
        cpu.setSoundTimer(-50000);
        stopLoop = true;
      }
      
      screen.update(thisCycle*25);
      delta = Sys.getTime()-start; //amount of time that calculations actually took
      
      if(loopCounter % 15 == 0)
      {
        int pauseTime = (int)(pauseScale*cycleTime-delta);
        if(pauseTime != 0)
          Display.sync(pauseTime);
        cycleTime = 0;
      }
      else if(loopCounter % 3 == 0)
      {
        try 
        {
          Thread.sleep(1);
        } catch (Exception ex) 
        {
          ex.printStackTrace();
        }
      }
      else
        Thread.yield();
    }
    synchronized(this)
    {
      this.notify();
    }
  }
  
  private float execute(int first, int second)
  {
    int cycleCount = 1; //this would be set in each instruction, but it is not variable for this cpu
    
    int part1 = ((first & 0xF0) >>> 4);
    int part2 = (first & 0x0F);
    int part3 = ((second & 0xF0) >>> 4);
    int part4 = (second & 0x0F);
    
    switch(part1) 
    {
      case 0x0:
      {
        switch (part4 | part3 << 4) 
        {
          case 0xE0:
            cls();
          break;
          case 0xEE:
            rts();
          break;
          case 0xFB:
            scright();
          break;
          case 0xFC:
            scleft();
          break;
          case 0xFE:
            low();
          break;
          case 0xFF:
            high();
          break;
          default:
          {
            if(part3 == 0xC)
              scdown(part4);
            else if((part3 << 4 | part4) == 0xFD)
            {
              JOptionPane.showMessageDialog(screen, "Game Over");
              reset();
            }
            else
              System.out.println("Invalid OpCode: " + (first << 8 | second) + " called");
          }
          break;
        }
      }
      break;
      case 0x1:
        jmp((part2 << 8 | part3 << 4 | part4));
      break;
      case 0x2:
        jsr((part2 << 8 | part3 << 4 | part4));
      break;
      case 0x3:
        skeq(part2, (part3 << 4 | part4));
      break;
      case 0x4:
        skne(part2, (part3 << 4 | part4));
      break;
      case 0x5:
        skeqReg(part2, part3);
      break;
      case 0x6:
        mov(part2, (part3 << 4 | part4));
      break;
      case 0x7:
        add(part2, (part3 << 4 | part4));
      break;
      case 0x8:
      {
        switch (part4) 
        {
          case 0x0:    
            movReg(part2, part3);
          break;
          case 0x1:    
            or(part2, part3);
          break;
          case 0x2:    
            and(part2, part3);
          break;
          case 0x3:    
            xor(part2, part3);
          break;
          case 0x4:    
            addReg(part2, part3);
          break;
          case 0x5:    
            sub(part2, part3);
          break;
          case 0x6:    
            shr(part2);
          break;
          case 0x7:    
            rsb(part2, part3);
          break;
          case 0xE:    
            shl(part2);
          break;
          default:
            System.out.println("Invalid OpCode: " + (first << 8 | second) + " called");
          break;
        }
      }
      break;
      case 0x9:    
        skneReg(part2, part3);
      break;
      case 0xA:    
        mvi((part2 << 8 | part3 << 4 | part4));
      break;
      case 0xB:    
        jmi((part2 << 8 | part3 << 4 | part4));
      break;
      case 0xC:    
        rand(part2, (part3 << 4 | part4));
      break;
      case 0xD:    
      {
        if(part4 != 0)
          sprite(part2, part3, part4);
        else
          xsprite(part2, part3);
      }
      break;
      case 0xE:    
      {
        switch (part4 | part3 << 4) 
        {
          case 0x9e:
            skpr(part2);
          break;
          case 0xA1:
            skup(part2);
          break;
          default:
            System.out.println("Invalid OpCode: " + (first << 8 | second) + " called");
          break;
        }
      }
      break;
      case 0xF:
      {
        switch (part3 << 4 | part4) 
        {
          case 0x07:
            gdelay(part2);
          break;
          case 0x0A:
            key(part2);
          break;
          case 0x15:
            sdelay(part2);
          break;
          case 0x18:
            ssound(part2);
          break;
          case 0x1E:
            adi(part2);
          break;
          case 0x29:
            font(part2);
          break;
          case 0x30:
            xfont(part2);
          break;
          case 0x33:
            bcd(part2);
          break;
          case 0x55:
            str(part2);
          break;
          case 0x65:
            ldr(part2);
          break;
          default:
            System.out.println("Invalid OpCode: " + (first << 8 | second) + " called");
          break;
        }
      }
      break;
      default:
        System.out.println("Invalid OpCode: " + (first << 8 | second) + " called");
      break;
    }
    
    return CYCLE_TIME * cycleCount;
  }
  
  private void scdown(int lines)
  {
    if(debug)
      System.out.println("Scroll down: " + lines + " lines.");
    screen.scrollDown(lines);
    screen.update();
  }
  
  private void cls()
  {
    if(debug)
      System.out.println("clearing screen");
    screen.clear();
    screen.update();
  }
  
  private void rts()
  {
    if(debug)
      System.out.println("Return from Subroutine");
    cpu.setProgramCounter(cpu.stackPop());
  }
  
  private void scright()
  {
    if(debug)
      System.out.println("Scroll right");
    screen.scrollRight();
    screen.update();
  }
  
  private void scleft()
  {
    if(debug)
      System.out.println("Scroll left");
    screen.scrollLeft();
    screen.update();
  }
  
  private void low()
  {
    System.out.println("Disable SChip");
    screen.setUseSChip(false);
    pauseScale = 150;
  }
  
  private void high()
  {
    System.out.println("Enable SChip");
    screen.setUseSChip(true);
    pauseScale = 250;
  }
  
  private void jmp(int address)
  {
    if(debug)
      System.out.println("Jumping To: " + Integer.toHexString(address));
    cpu.setProgramCounter(address-2);
  }
  
  private void jsr(int address)
  {
    if(debug)
      System.out.println("Subroutine: " + Integer.toHexString(address));
    cpu.stackPush(cpu.getProgramCounter());
    cpu.setProgramCounter(address-2);
  }
  
  private void skeq(int r1, int constant)
  {
    if(debug)
      System.out.println("skip if constant: " + Integer.toHexString(constant) + ", equals r" + Integer.toHexString(r1) + ": " + cpu.getRegisterValue(r1));
    if(cpu.getRegisterValue(r1) == constant)
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
  }
  
  private void skne(int r1, int constant)
  {
    if(debug)
      System.out.println("Skip if not equal, r" + Integer.toHexString(r1) + ": " + Integer.toHexString(cpu.getRegisterValue(r1)) + ", constant: " + Integer.toHexString(constant));
    if(cpu.getRegisterValue(r1) != constant)
    {
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
    }
  }
  
  private void skeqReg(int r1, int r2)
  {
    if(debug)
      System.out.println("Skip if not equal, r" + Integer.toHexString(r1) + ": " + Integer.toHexString(cpu.getRegisterValue(r1)) + ", r" + Integer.toHexString(r2) + ": " + Integer.toHexString(r2));
    if(cpu.getRegisterValue(r1) == cpu.getRegisterValue(r2))
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
  }
  
  private void mov(int r1, int constant)
  {
    if(debug)
      System.out.println("Setting r" + Integer.toHexString(r1) + " to constant: " + Integer.toHexString(constant));
    cpu.setRegister(r1, constant);
  }
  
  private void add(int r1, int constant)
  { 
    if(debug)
      System.out.println("adding constant: " + Integer.toHexString(constant) + " to r" + Integer.toHexString(r1) + "(current value: " + cpu.getRegisterValue(r1) + ")");
    cpu.setRegister(r1, (cpu.getRegisterValue(r1) + constant) & 0xFF);
  }
  
  private void movReg(int r1, int r2)
  {
    if(debug)
      System.out.println("moving: r" + Integer.toHexString(r2) + ": " + cpu.getRegisterValue(r2) + " to r" + Integer.toHexString(r1));
    cpu.setRegister(r1, cpu.getRegisterValue(r2));
  }
  
  private void or(int r1, int r2)
  {
    if(debug)
      System.out.println("ORing r" + Integer.toHexString(r1) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r1)) + " with r" + Integer.toHexString(r2) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r2)));
    cpu.setRegister(r1, (cpu.getRegisterValue(r1) | cpu.getRegisterValue(r2)));
    if(debug)
      System.out.println("Result: " + Integer.toBinaryString(cpu.getRegisterValue(r1)));
  }
  
  private void and(int r1, int r2)
  {
    if(debug)
      System.out.println("ANDing r" + Integer.toHexString(r1) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r1)) + " with r" + Integer.toHexString(r2) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r2)));
    cpu.setRegister(r1, (cpu.getRegisterValue(r1) & cpu.getRegisterValue(r2)));
    if(debug)
      System.out.println("Result: " + Integer.toBinaryString(cpu.getRegisterValue(r1)));
  }
  
  private void xor(int r1, int r2)
  {
    if(debug)
      System.out.println("XORing r" + Integer.toHexString(r1) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r1)) + " with r" + Integer.toHexString(r2) + ": " + Integer.toBinaryString(cpu.getRegisterValue(r2)));
    cpu.setRegister(r1, (cpu.getRegisterValue(r1) ^ cpu.getRegisterValue(r2)));
    if(debug)
      System.out.println("Result: " + Integer.toBinaryString(cpu.getRegisterValue(r1)));
  }
  
  private void addReg(int r1, int r2)
  {
    if(debug)
      System.out.println("adding: r" + Integer.toHexString(r2) + ": " + Integer.toHexString(cpu.getRegisterValue(r2)) + " to r" + Integer.toHexString(r1) + " (current value: " + Integer.toHexString(cpu.getRegisterValue(r1)) + ")");
    int newVal = cpu.getRegisterValue(r1) + cpu.getRegisterValue(r2);
    int carry = (newVal & 0x0100) >>> 8;
    if(newVal < 0)
      carry = 0;
    newVal = newVal & 0xFF;
    
    cpu.setRegister(r1, newVal);
    cpu.setRegister(CPU.VF, carry);
  }
  
  private void sub(int r1, int r2)
  {
    if(cpu.getRegisterValue(r1) >= cpu.getRegisterValue(r2))
      cpu.setRegister(CPU.VF, 0x1);
    else
      cpu.setRegister(CPU.VF, 0x0);
    
    if(debug)
      System.out.println("subtracting r" + Integer.toHexString(r2) + ": " + Integer.toHexString(cpu.getRegisterValue(r2)) + " from r" + Integer.toHexString(r1) + " (current value: " + Integer.toHexString(cpu.getRegisterValue(r1)) + ")");
    int result = (cpu.getRegisterValue(r1) -cpu.getRegisterValue(r2));
    cpu.setRegister(r1, result & 0xFF);
  }
  
  private void shr(int r1)
  {
    if(debug)
      System.out.println("right shifting r" + Integer.toHexString(r1) + " current value: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    cpu.setRegister(CPU.VF, (cpu.getRegisterValue(r1) & 0x1));
    cpu.setRegister(r1, (cpu.getRegisterValue(r1) >>> 1));
  }
  
  private void rsb(int r1, int r2)
  {
    if(cpu.getRegisterValue(r2) >= cpu.getRegisterValue(r1))
      cpu.setRegister(CPU.VF, 0x1);
    else
      cpu.setRegister(CPU.VF, 0x0);
    
    if(debug)
      System.out.println("subtracting: " + Integer.toHexString(cpu.getRegisterValue(r1)) + "from r" + Integer.toHexString(r2) + " (current value: " + Integer.toHexString(cpu.getRegisterValue(r2)) + ")");
    int result = (cpu.getRegisterValue(r2) - cpu.getRegisterValue(r1));
    cpu.setRegister(r1, result & 0xFF);
  }
  
  private void shl(int r1)
  {
    if(debug)
      System.out.println("left shifting r" + Integer.toHexString(r1) + " current value: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    cpu.setRegister(CPU.VF, (cpu.getRegisterValue(r1) & 0x80) >>> 7);
    cpu.setRegister(r1, ((cpu.getRegisterValue(r1) << 1) & 0xFF));
  }
  
  private void skneReg(int r1, int r2)
  {
    if(debug)
      System.out.println("skip if not equal, r" + Integer.toHexString(r1) + " equals: " + Integer.toHexString(cpu.getRegisterValue(r1)) + " r" + Integer.toHexString(r2) + " equals: " + Integer.toHexString(cpu.getRegisterValue(r2)));
    if(cpu.getRegisterValue(r1) != cpu.getRegisterValue(r2))
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
  }
  
  private void mvi(int constant)
  {
    if(debug)
      System.out.println("set Index register to: " + Integer.toHexString(constant));
    cpu.setAddressRegister(constant);
  }
  
  private void jmi(int constant)
  {
    if(debug)
      System.out.println("move program counter to: " + Integer.toHexString((cpu.getRegisterValue(0) + constant)));
    cpu.setProgramCounter((cpu.getRegisterValue(0) + constant - 2));
  }
  
  private void rand(int r1, int constant)
  {
    int rand = (int)(Math.random()*255) & constant;
    if(debug)
      System.out.println("setting r" + Integer.toHexString(r1) + " to random value: " + Integer.toHexString(rand) + " max rand: " + constant);
    cpu.setRegister(r1, rand);
  }
  
  private void sprite(int r1, int r2, int height)
  { 
    if(debug)
      System.out.println("drawing sprite at r" + Integer.toHexString(r1) + ": " + "x(decimal): " + cpu.getRegisterValue(r1) + ", r" + Integer.toHexString(r2) + ": y(decimal): + " + cpu.getRegisterValue(r2) + ", height: (decimal)" + height);
  
    drawSprite(r1, r2, height, 8);
  }
  
  private void drawSprite(int r1, int r2, int height, int width)
  {
    int[] pixels = new int[width*height];
    int rowIndex = 0;
    
    for (int i = 0; i < pixels.length/width; i++) 
    {
      int row = memory.load(cpu.getAddressRegister() + rowIndex++);
      if(width > 8)
      {
        int row2 = memory.load(cpu.getAddressRegister() + rowIndex++);
        row = row2 | row << 8;
      }
      
      int pixelIndex = i*width;
      int mask = (int)Math.pow(2, width);
      int count = width;
      
      for (int j = 0; j < width; j++)
      {
        mask /= 2;
        count--;
        pixels[pixelIndex + j] = (row & mask) >>> count;
      }
    }
    
    boolean collision = screen.drawSprite(cpu.getRegisterValue(r1), cpu.getRegisterValue(r2),
                                          width, pixels);
    if(collision)
    {
      if(debug)
        System.out.println("Collision Occurred!");
      cpu.setRegister(CPU.VF, 0x1);
    }
    else
      cpu.setRegister(CPU.VF, 0x0);
      
    screen.update();
  }
  
  private void xsprite(int r1, int r2)
  {
    if(debug)
      System.out.println("drawing X-sprite at r" + Integer.toHexString(r1) + ": " + "x(decimal): " + cpu.getRegisterValue(r1) + ", r" + Integer.toHexString(r2) + ": y(decimal): + " + cpu.getRegisterValue(r2));
    drawSprite(r1, r2, 16, 16);
  }
  
  private void skpr(int r1)
  {
    if(debug)
      System.out.println("skip if key down");
    if(isKeyPressed(cpu.getRegisterValue(r1)))
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
  }
  
  private void skup(int r1)
  {
    if(debug)
      System.out.println("skip if key up");
    if(!isKeyPressed(cpu.getRegisterValue(r1)))
      cpu.setProgramCounter(cpu.getProgramCounter() + 2);
  }
  
  private void gdelay(int r1)
  {
    if(debug)
      System.out.println("setting r" + Integer.toHexString(r1) + " to delay-timer: " + Integer.toHexString(cpu.getDelayTimer()));
    cpu.setRegister(r1, cpu.getDelayTimer());
  }
  
  private void key(int r1)
  {
    if(debug)
      System.out.println("waiting for key press");
    if(getPressedKey() == -1)
      cpu.setProgramCounter(cpu.getProgramCounter()-2);
    else
      cpu.setRegister(r1, CHIP8.intToHex(getPressedKey()));
  }
  
  private void sdelay(int r1)
  {
    if(debug)
    System.out.println("setting delay timer to: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    cpu.setDelayTimer(cpu.getRegisterValue(r1));
  }
  
  private void ssound(int r1)
  {
    if(debug)
      System.out.println("setting sound timer to: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    cpu.setSoundTimer(cpu.getRegisterValue(r1));
    if(!looping || stopLoop)
      loop();
  }
  
  private void adi(int r1)
  {
    if(debug)
      System.out.println("add " + Integer.toHexString(cpu.getRegisterValue(r1)) + " to Index register (current value: " + Integer.toHexString(cpu.getAddressRegister()) + ")");
    cpu.setAddressRegister((cpu.getAddressRegister() + cpu.getRegisterValue(r1)));
  }
  
  private void font(int r1)
  {
    if(debug)
      System.out.println("load font sprite: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    
    cpu.setAddressRegister(5*CHIP8.hexToInt(CHIP8.intToHex(cpu.getRegisterValue(r1))));
  }
  
  private void xfont(int r1)
  {
    if(debug)
      System.out.println("load X-font sprite: " + Integer.toHexString(cpu.getRegisterValue(r1)));
    
    cpu.setAddressRegister(79 + 10*CHIP8.hexToInt(CHIP8.intToHex(cpu.getRegisterValue(r1))));
  }
  
  private void bcd(int r1)
  {
    int num = cpu.getRegisterValue(r1);
    int ones = (num%10);
    int tens = (num%100/10);
    int hundreds = (num%1000/100);
    
    if(debug)
    {
      System.out.println("BCDing " + Integer.toHexString(cpu.getRegisterValue(r1)) + " to: " + "hundreds: " + hundreds + " tens: " + tens + " ones: " + ones);
      System.out.println("Storing BCD at: " + Integer.toHexString(cpu.getAddressRegister()) + ", " + Integer.toHexString(cpu.getAddressRegister()+1) + ", " + Integer.toHexString(cpu.getAddressRegister()+2));
    }
    
    memory.store(cpu.getAddressRegister(), hundreds);
    memory.store(cpu.getAddressRegister()+1, tens);
    memory.store(cpu.getAddressRegister()+2, ones);
  }
  
  private void str(int r1)
  {
    if(debug)
      System.out.println("storing registers v0 to v" + r1 + " starting at: " + Integer.toHexString(cpu.getAddressRegister()));
    for (int i = 0; i < r1+1; i++) 
    {
      memory.store(cpu.getAddressRegister()+i, cpu.getRegisterValue(i)); 
      //cpu.setAddressRegister(cpu.getAddressRegister()+1); //not clear whether I should be incremented or not
    }
  }
  
  private void ldr(int r1)
  {
    if(debug)
      System.out.println("loading registers v0 to v" + r1 + " with data starting at: " + Integer.toHexString(cpu.getAddressRegister()));
    for (int i = 0; i < r1+1; i++) 
    {
      if(debug)
        System.out.println("Mem: " + memory.load(cpu.getAddressRegister()));
      cpu.setRegister(i, memory.load(cpu.getAddressRegister()+i));
      //cpu.setAddressRegister(cpu.getAddressRegister()+1); //not clear whether I should be incremented or not
    }
  }

  public boolean isPaused()
  {
    return paused;
  }

  public void setPaused(boolean paused)
  {
    this.paused = paused;
    screen.setPaused(paused);
  }

  public boolean isSoundEnabled()
  {
    return soundEnabled;
  }

  public void setSoundEnabled(boolean soundEnabled)
  {
    this.soundEnabled = soundEnabled;
  }

  public Screen getScreen()
  {
    return screen;
  }

  public void setScreen(Screen screen)
  {
    this.screen = screen;
  }
}