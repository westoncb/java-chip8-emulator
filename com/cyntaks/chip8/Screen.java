package com.cyntaks.chip8;

import com.cyntaks.chip8.utils.Texture;
import com.cyntaks.chip8.utils.TextureLoader;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.glu.GLU;

public class Screen extends AWTGLCanvas
{
  private int[] data;
  private int[] renderData;
  private int[] deleteBuffer;
  private int[] timers;
  
  private int width;
  private int height;
  private int scale;
  
  private Texture pixel;
  private Texture pixel2;
  private Texture pixel3;
  private Texture pausedTexture;
  private boolean paused;
  private boolean reduceFlicker;
  private int deleteDelay = 60;
  private float trailOpacity = 1.2f;
    
  private boolean loaded;
  
  private float[][] color;
  private float[][] lastColor;
  private float[] time;
  private float[] lastTime;
  private float[] positions;
  
  private static final int NUM_POSITIONS = 4;
  private static final int RED = 0;
  private static final int GREEN = 1;
  private static final int BLUE = 2;
  
  private boolean useBlur;
  private boolean useColorFading;
  
  private boolean schip;
  
  public Screen() throws LWJGLException
  {
    init(64, 32);
    
    reduceFlicker = true;
    useBlur = true;
    useColorFading = true;
    
    ComponentListener listener = new ComponentAdapter(){
      public void componentResized(ComponentEvent e)
      {
        loaded = false;
      }
    };
    
    this.addComponentListener(listener);
    setSize(width*12, height*12);
    reset();
  }
  
  public void setUseSChip(boolean schip)
  {
    this.schip = schip;
    
    if(schip)
      init(128, 64);
    else
      init(64, 32);
  }
  
  private void init(int width, int height)
  {
    this.width = width;
    this.height = height;
    this.scale = (int)((64f/width)*12);
    
    data = new int[width*(height+1)];
    renderData = new int[data.length];
    deleteBuffer = new int[data.length];
    timers = new int[data.length];
    
    for (int i = 0; i < timers.length; i++) 
    {
      timers[i] = -1;
    }
    
    color = new float[NUM_POSITIONS][3];
    lastColor = new float[NUM_POSITIONS][3];
    time = new float[NUM_POSITIONS];
    lastTime = new float[NUM_POSITIONS];
    positions = new float[NUM_POSITIONS];
    
    for (int i = 0; i < NUM_POSITIONS; i++) 
    {
      float segSize = (width*scale)/(float)(NUM_POSITIONS-1);
      positions[i] = segSize*i;
      color[i][RED] = (float)Math.random();
      color[i][GREEN] = (float)Math.random();
      color[i][BLUE] = (float)Math.random();
                
      timeUp(i);
    }
  }
  
  private void loadImages()
  {
    try 
    {
      pixel = TextureLoader.getTexture("images/pixel.png");
      pixel2 = TextureLoader.getTexture("images/pixel2.png"); 
      pixel3 = TextureLoader.getTexture("images/pixel3.png");
      pausedTexture = TextureLoader.getTexture("images/paused.png");
    } catch (IOException ex) 
    {
      ex.printStackTrace();
    }
  }
  
  public boolean setPixel(int row, int col, int value)
  {
    int index = row*width + col;
    if(value == 1)
    {
      int newVal = data[index] ^ 1;
      if(newVal == 0)
      {
        data[index] = 0;
        deleteBuffer[index] = 1;
        timers[index] = deleteDelay;
        if(!reduceFlicker)
          renderData[index] = 0;
        return true;
      }
      else
      {
        deleteBuffer[index] = 0;
        data[index] = 1;
        if(row < this.height)
          renderData[index] = 1;
        timers[index] = -1;
        return false;
      }
    }
    else
      return false;
  }
  
  public void setAbsolutePixelValue(int index, int value)
  {
    if(index < 128*64)
    {
      data[index] = value;
      deleteBuffer[index] = 0;
      timers[index] = -1;
      renderData[index] = value;
    }
  }
  
  public void updateDeleteBuffer()
  {
    for (int i = 0; i < timers.length; i++) 
    {
      if(deleteBuffer[i] == 1 && timers[i] != -1) 
      {
        timers[i]--;
        if(timers[i] == 0)
        {
          deleteBuffer[i] = 0;
          renderData[i] = 0;
          timers[i] = -1;
        }
      }
    }
  }
  
  public int getPixel(int row, int col)
  {
    return data[row*width + col];
  }
  
  public boolean drawSprite(int xOff, int yOff, int width, int[] pixels)
  { 
    boolean collision = false;
    for (int i = 0; i < pixels.length; i++) 
    {
      int x = (xOff + i%width)%this.width;
      int y = (yOff + i/width)%(this.height+1);
      
      if(setPixel(y, x, pixels[i])) 
        collision = true;
    }
    
    return collision;
  }
  
  public void scrollDown(int lines)
  {
    int elements = this.width*lines;
    
    for (int i = data.length-elements-1; i > -1; i--) 
    {
      setAbsolutePixelValue(i+elements, data[i]);
    }
    for (int i = 0; i < elements; i++) 
    {
      setAbsolutePixelValue(i, 0);
    }
  }
  
  public void scrollLeft()
  {
    for (int i = 0; i < 4; i++) 
    {
      for (int j = 0; j < this.height; j++) 
      {
        int start = j*this.width;
        for (int k = 0; k < this.width-1; k++) 
        {
          setAbsolutePixelValue(start+k, data[start+k+1]);
        }
        setAbsolutePixelValue(start+this.width-1, 0);
      }
    }
  }
  
  public void scrollRight()
  {
    for (int i = 0; i < 4; i++) 
    {
      for (int j = 0; j < this.height; j++) 
      {
        int start = j*this.width;
        for (int k = this.width-1; k > 0; k--) 
        {
          setAbsolutePixelValue(start+k, data[start+k-1]);
        }
        setAbsolutePixelValue(start, 0);
      }
    }
  }
  
  public void update()
  {
    repaint();
  }
  
  public void paintGL()
  {
    if(!loaded)
    {
      try 
      {
        makeCurrent();
        setVSyncEnabled(true);
      } catch (LWJGLException ex) 
      {
        ex.printStackTrace();
      }
      loadImages();
      GL11.glViewport(0, 0, getWidth(), getHeight());
            
      GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
          
      loaded = true;
    }
    
    GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
        GLU.gluOrtho2D(0, width*scale, height*scale, 0);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glLoadIdentity();
        
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f(1, 1, 1, 1);
    if(useBlur)
    {
      if(useColorFading)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pixel3.getTextureID());
      else
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pixel.getTextureID());
    }
    else
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, pixel2.getTextureID());
    GL11.glBegin(GL11.GL_QUADS);
    
    for (int i = 0; i < renderData.length; i++) 
    {
      if(renderData[i] != 0)
      {
        float scale = this.scale;
        float x = i%width*scale-10;
        float y = i/width*scale-10;
        int colorIndex = 0;
        for (int j = 0; j < positions.length-1; j++) 
        {
          if(x >= positions[j] && x <= positions[j+1])
          {
            colorIndex = j;
            j = positions.length;
          }
        }
        
        float relPosition = (x-positions[colorIndex])/(float)(positions[colorIndex+1]-positions[colorIndex]);
        
        float[] color1 = getInterpolatedColor(colorIndex);
        float[] color2 = getInterpolatedColor(colorIndex+1);
        float[] color = new float[3];
        
        color[RED] = (1-relPosition)*color1[RED] + relPosition*color2[RED];
        color[GREEN] = (1-relPosition)*color1[GREEN] + relPosition*color2[GREEN];
        color[BLUE] = (1-relPosition)*color1[BLUE] + relPosition*color2[BLUE];
        
        float alpha = 1;
        if(timers[i] != -1)
          alpha = (float)(timers[i]*trailOpacity)/deleteDelay;
        
        if(useColorFading)
          GL11.glColor4f(color[0], color[1], color[2], alpha);
        else
          GL11.glColor4f(1, 1, 1, alpha);
          
        float start = 0f;
        float end = 1f;
        float width = 32*(scale/12f);
        GL11.glTexCoord2f(start, end);
        GL11.glVertex2f(x, y+width);
        GL11.glTexCoord2f(start, start);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(end, start);
        GL11.glVertex2f(x+width, y);
        GL11.glTexCoord2f(end, end);
        GL11.glVertex2f(x+width, y+width);
      }
    }
    GL11.glEnd();
    
    if(paused)
      renderPaused();
    
    try 
    {
      swapBuffers();
    } catch (LWJGLException ex) 
    {
      ex.printStackTrace();
    }
  }
  
  private void renderPaused()
  { 
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(.7f, .7f, .7f, .7f);
    GL11.glPushMatrix();
      GL11.glTranslatef(width*scale/2 - 150,
                        height*scale/2 - 62,
                        0);
      GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 125);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(300, 0);
        GL11.glVertex2f(300, 125);
      GL11.glEnd();
      GL11.glColor4f(.8f, .8f, .8f, 1);  
      GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(0, 125);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(300, 0);
        GL11.glVertex2f(300, 125);
      GL11.glEnd();
    GL11.glPopMatrix();
    
    float xStart = 0;
    float xEnd = pausedTexture.getImageWidth()/(float)pausedTexture.getTextureWidth();
    float yStart = 0;
    float yEnd = pausedTexture.getImageHeight()/(float)pausedTexture.getTextureHeight();
    
    pausedTexture.bind();
    GL11.glColor4f(1, 1, 1, 1);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glPushMatrix();
      GL11.glTranslatef(width*scale/2 - 91,
                        height*scale/2 - 26,
                        0);
      GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(xStart, yEnd); GL11.glVertex2f(0, 53);
        GL11.glTexCoord2f(xStart, yStart); GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(xEnd, yStart); GL11.glVertex2f(182, 0);
        GL11.glTexCoord2f(xEnd, yEnd); GL11.glVertex2f(182, 53);
      GL11.glEnd();
    GL11.glPopMatrix();
  }
  
  public void update(float delta)
  {
    delta /= 100f;
    for (int i = 0; i < NUM_POSITIONS; i++) 
    {
      time[i] -= delta;
      if(time[i] < 0)
        timeUp(i);
    }
    
    updateDeleteBuffer();
  }
  
  public void clear()
  {
    for (int i = 0; i < data.length; i++) 
    {
      data[i] = 0;
      renderData[i] = 0;
      timers[i] = 0;
      deleteBuffer[i] = 0;
    }
  }
  
  private float[] getInterpolatedColor(int position)
    {
        float lastRed = lastColor[position][RED];
        float lastGreen = lastColor[position][GREEN];
        float lastBlue = lastColor[position][BLUE];
        
        float red = color[position][RED];
        float green = color[position][GREEN];
        float blue = color[position][BLUE];
        
        float done = (lastTime[position]-time[position])/lastTime[position];
        float[] colorBuffer = new float[3];
        colorBuffer[RED] = (1-done)*lastRed + done*red;
        colorBuffer[GREEN] = (1-done)*lastGreen + done*green;
        colorBuffer[BLUE] = (1-done)*lastBlue + done*blue;
        
        return colorBuffer;
    }
    
  private void timeUp(int position)
    {
        lastTime[position] = (float)(Math.random()*5+2);
        time[position] = lastTime[position];
        
        float red = (float)Math.random()*(255-110)+110;
        float green = (float)Math.random()*(255-110)+110;
        float blue = (float)Math.random()*(255-110)+110;
        
        red /= 255f;
        green /= 255f;
        blue /= 255f;
        
        lastColor[position][RED] = color[position][RED];
        lastColor[position][GREEN] = color[position][GREEN];
        lastColor[position][BLUE] = color[position][BLUE];
        
        color[position][RED] = red;
        color[position][GREEN] = green;
        color[position][BLUE] = blue;
    }
  
  public void reset()
  {
    clear();
  }
  
  public void reload()
  {
    loaded = false;
  }

  public boolean isPaused()
  {
    return paused;
  }

  public void setPaused(boolean paused)
  {
    this.paused = paused;
    update();
  }

  public boolean isReduceFlicker()
  {
    return reduceFlicker;
  }

  public void setReduceFlicker(boolean reduceFlicker)
  {
    this.reduceFlicker = reduceFlicker;
  }

  public int getScale()
  {
    return scale;
  }

  public int getPixelsAcross()
  {
    return width;
  }

  public int getPixelsHigh()
  {
    return height;
  }

  public int[] getData()
  {
    return data;
  }

  public void setData(int[] data)
  {
    this.data = data;
  }

  public int[] getRenderData()
  {
    return renderData;
  }

  public void setRenderData(int[] renderData)
  {
    this.renderData = renderData;
  }

  public int[] getTimers()
  {
    return timers;
  }

  public void setTimers(int[] timers)
  {
    this.timers = timers;
  }

  public int[] getDeleteBuffer()
  {
    return deleteBuffer;
  }

  public void setDeleteBuffer(int[] deleteBuffer)
  {
    this.deleteBuffer = deleteBuffer;
  }

  public boolean isUseBlur()
  {
    return useBlur;
  }

  public void setUseBlur(boolean useBlur)
  {
    this.useBlur = useBlur;
    if(useBlur)
    {
      deleteDelay = 60;
      trailOpacity = 1.1f;
    }
    else
    {
      deleteDelay = 15;
      trailOpacity = 1.5f;
    }
  }

  public boolean isUseColorFading()
  {
    return useColorFading;
  }

  public void setUseColorFading(boolean useColorFading)
  {
    this.useColorFading = useColorFading;
  }

  public boolean isSchip()
  {
    return schip;
  }
}