package com.cyntaks.chip8;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.*;
import org.lwjgl.LWJGLException;

public class CHIP8 implements KeyListener
{
  private Memory memory;
  private CPU cpu;
  private CPUCore cpuCore;
  private Screen screen;
  private File lastRom;
    
  private static final int TOTAL_MEMORY = 4096;
  
  private static final int SCREEN_WIDTH = 64;
  private static final int SCREEN_HEIGHT = 32;
  private static final int SUPER_SCREEN_WIDTH = 128;
  private static final int SUPER_SCREEN_HEIGHT = 64;
  
  public CHIP8(boolean useSuper, int pixelScale)
  {
    memory = new Memory(TOTAL_MEMORY);
    cpu = new CPU();
    
    try 
    {
      screen = new Screen();
    } catch (LWJGLException ex) 
    {
      ex.printStackTrace();
    }
    
    cpuCore = new CPUCore(cpu, memory, screen, this);
  }
  
  public void loadProgram(File file)
  {
    memory.reset();
    screen.setUseSChip(false);
    loadFont();
    lastRom = file;
    
    try 
    {
      FileInputStream fi = new FileInputStream(file);
      BufferedInputStream bi = new BufferedInputStream(fi);
      DataInputStream dataIn = new DataInputStream(bi);
      
      int curByte = 0;
      int counter = 0;
      
      try 
      {
        while((curByte = dataIn.read()) != -1)
        {
          int theByte = curByte & 0xFF;
          memory.store(Memory.PROGRAM_START + counter, theByte);
          counter++;
        }
        dataIn.close();
      } catch (IOException ex) 
      {
        ex.printStackTrace();
      }
      
      cpuCore.beginExecution();
    } catch (FileNotFoundException ex) 
    {
      ex.printStackTrace();
    }
  }
  
  private void loadFont()
  {
    int[] fontTable = { 0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                        0x20, 0x60, 0x20, 0x20, 0x70, // 1
                        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
                       };
    
    for (int i = 0; i < fontTable.length; i++) 
    {
      memory.store(i, fontTable[i]);
    }
    
    int[] schipFont = new int[] { 
        0xF0, 0xF0, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0xF0, 0xF0, // 0
        0x20, 0x20, 0x60, 0x60, 0x20, 0x20, 0x20, 0x20, 0x70, 0x70, // 1
        0xF0, 0xF0, 0x10, 0x10, 0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, // 2
        0xF0, 0xF0, 0x10, 0x10, 0xF0, 0xF0, 0x10, 0x10, 0xF0, 0xF0, // 3
        0x90, 0x90, 0x90, 0x90, 0xF0, 0xF0, 0x10, 0x10, 0x10, 0x10, // 4
        0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, 0x10, 0x10, 0xF0, 0xF0, // 5
        0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, 0x90, 0x90, 0xF0, 0xF0, // 6
        0xF0, 0xF0, 0x10, 0x10, 0x20, 0x20, 0x40, 0x40, 0x40, 0x40, // 7
        0xF0, 0xF0, 0x90, 0x90, 0xF0, 0xF0, 0x90, 0x90, 0xF0, 0xF0, // 8
        0xF0, 0xF0, 0x90, 0x90, 0xF0, 0xF0, 0x10, 0x10, 0xF0, 0xF0, // 9
        0xF0, 0xF0, 0x90, 0x90, 0xF0, 0xF0, 0x90, 0x90, 0x90, 0x90, // A
        0xE0, 0xE0, 0x90, 0x90, 0xE0, 0xE0, 0x90, 0x90, 0xE0, 0xE0, // B
        0xF0, 0xF0, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xF0, 0xF0, // C
        0xE0, 0xE0, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0xE0, 0xE0, // D
        0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, // E
        0xF0, 0xF0, 0x80, 0x80, 0xF0, 0xF0, 0x80, 0x80, 0x80, 0x80  // F
    };
    
    for (int i = 79; i < 239; i++) 
    {
      memory.store(i, schipFont[i-79]);
    }
  }
  
  public static int hexToInt(char character)
  {
    character = Character.toUpperCase(character);
    if(Character.isDigit(character))
      return character-'0';
    else
      return 10+(character-'A');
  }
  
  public static char intToHex(int digit)
  {
    if(digit < 10)
      return (char)('0' + digit);
    else
      return (char)('A'-10+digit);
  }
  
  public void keyPressed(KeyEvent e)
  {
    if(e.getKeyCode() == KeyEvent.VK_SPACE)
       cpuCore.setPaused(!cpuCore.isPaused());
    else if(e.getKeyChar() >= '0' && e.getKeyChar() <= '9' ||
            e.getKeyChar() >= 'a' && e.getKeyChar() <= 'f')
    {
      char character = e.getKeyChar();
      
      if(character == '7')
        character = '1';
      else if(character == '1')
        character = '7';
      else if(character == '8')
        character = '2';
      else if(character == '2')
        character = '8';
      else if(character == '9')
        character = '3';
      else if(character == '3')
        character = '9';
        
      cpuCore.keyPressed(character); 
    }
  }
  
  public void keyReleased(KeyEvent e)
  {
    if(e.getKeyChar() >= '0' && e.getKeyChar() <= '9' ||
            e.getKeyChar() >= 'a' && e.getKeyChar() <= 'f')
    {
      char character = e.getKeyChar();
      
      if(character == '7')
        character = '1';
      else if(character == '1')
        character = '7';
      else if(character == '8')
        character = '2';
      else if(character == '2')
        character = '8';
      else if(character == '9')
        character = '3';
      else if(character == '3')
        character = '9';
        
      cpuCore.keyReleased(character); 
    }
  }
  
  public void keyTyped(KeyEvent e){}
  
  public CPUCore getCpuCore()
  {
    return cpuCore;
  }
  
  public void reset()
  {
    cpu.reset();
    cpuCore.reset();
    screen.reset();
    memory.reset();
  }
  
  public void loadLastRom()
  {
    if(lastRom != null);
      loadProgram(lastRom);
  }

  public Screen getScreen()
  {
    return screen;
  }
  
  public void recreateScreen()
  {
    try 
    {
      int[] data = screen.getData();
      int[] renderData = screen.getRenderData();
      int[] timers = screen.getTimers();
      int[] deleteBuffer = screen.getDeleteBuffer();
      boolean paused = screen.isPaused();
      boolean reduceFlicker = screen.isReduceFlicker();
      boolean useColorFading = screen.isUseColorFading();
      boolean useBlur = screen.isUseBlur();
      boolean useSChip = screen.isSchip();
      
      screen = new Screen();
      screen.setUseSChip(useSChip);
      screen.setData(data);
      screen.setRenderData(renderData);
      screen.setTimers(timers);
      screen.setDeleteBuffer(deleteBuffer);
      screen.setPaused(paused);
      screen.setReduceFlicker(reduceFlicker);
      screen.setUseColorFading(useColorFading);
      screen.setUseBlur(useBlur);
      
      cpuCore.setScreen(screen);
    } catch (LWJGLException ex) 
    {
      ex.printStackTrace();
    }
  }
}