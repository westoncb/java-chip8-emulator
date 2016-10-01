package com.cyntaks.chip8;

public class CPU 
{
  public static final int STACK_SIZE = 16;
  public static final int NUM_REGISTERS = 16;
  
  private int[] dataRegisters;
  private int addressRegister;
  private int stackPointer;
  private int programCounter;
  private int[] stack;
  
  private int delayTimer;
  private int soundTimer;
  
  public static final int VF = 15;
  
  public CPU()
  {
    dataRegisters = new int[NUM_REGISTERS];   
    stack = new int[STACK_SIZE];
    reset();
  }
  
  public void reset()
  {
    for (int i = 0; i < dataRegisters.length; i++) 
    {
      dataRegisters[i] = 0; 
    }
    
    for (int i = 0; i < stack.length; i++) 
    {
      stack[i] = 0; 
    }
    
    addressRegister = 0;
    stackPointer = STACK_SIZE-1;
    programCounter = Memory.PROGRAM_START;
  }
  
  public void stackPush(int data)
  {
    stack[stackPointer] = data;
    stackPointer--;
    
    if(stackPointer < 0)
      System.err.println("Stack Overflow!");
  }
  
  public int stackPop()
  {
    stackPointer++;
    if(stackPointer == STACK_SIZE)
    {
      System.err.println("Stack Underflow!");
      return 0;
    }
    else
      return stack[stackPointer];
  }
  
  public void setRegister(int register, int value)
  {
    dataRegisters[register] = value;
  }
  
  public int getRegisterValue(int register)
  {
    return dataRegisters[register];
  }

  public int getAddressRegister()
  {
    return addressRegister;
  }

  public void setAddressRegister(int addressRegister)
  {
    this.addressRegister = addressRegister;
  }

  public int getProgramCounter()
  {
    return programCounter;
  }

  public void setProgramCounter(int programCounter)
  {
    this.programCounter = programCounter;
  }

  public int getDelayTimer()
  {
    return delayTimer;
  }

  public void setDelayTimer(int delayTimer)
  {
    this.delayTimer = delayTimer;
  }

  public int getSoundTimer()
  {
    return soundTimer;
  }

  public void setSoundTimer(int soundTimer)
  {
    this.soundTimer = soundTimer;
  }
}