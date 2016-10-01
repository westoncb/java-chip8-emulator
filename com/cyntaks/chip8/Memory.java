package com.cyntaks.chip8;

public class Memory 
{
  public static final int PROGRAM_START = 0x200;
  
  private int[] contents;
  
  public Memory(int numBytes)
  {
    contents = new int[numBytes];
    reset();
  }
  
  public int load(int address)
  {
    return contents[address];
  }
  
  public void store(int address, int data)
  {
    contents[address] = data;
  }
  
  public void reset()
  {
    for (int i = 0; i < contents.length-PROGRAM_START; i++) 
    {
      contents[PROGRAM_START + i] = 0; 
    }
  }
  
  public void dump()
  {
    int counter = 0;
    int instruction = 0;
    
    for (int i = 0; i < contents.length; i++) 
    {
      int theByte = contents[i];
      counter++;
      if(counter % 2 == 0)
      {
        instruction = instruction | theByte;
        if(instruction == 0)
          break;
        else
          System.out.println(i/2+1 + ": " + Integer.toHexString(instruction));
      }
      else
      {
        instruction = theByte << 8;
      }
    }
  }
}