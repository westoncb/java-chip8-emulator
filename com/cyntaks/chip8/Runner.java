package com.cyntaks.chip8;

import com.cyntaks.chip8.utils.TextureLoader;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;

public class Runner implements ActionListener, FocusListener
{  
  private JFrame mainFrame;
  private JMenuBar menu;
  private JMenu file;
  private JMenu options;
  
  //file
  private JMenuItem exit;
  private JMenuItem load;
  private JMenuItem reset;
  
  //options
  private JMenuItem sound;
  private JMenuItem reduceFlicker;
  private JMenuItem colorFading;
  private JMenuItem plainTiles;
  
  private CHIP8 chip8;
  
  private boolean wasPaused;
  private boolean canvasRemoved;
  
  private KeyListener keyListener;
  
  private int oldWidth;
  private int oldHeight;
  private int oldFrameWidth;
  private int oldFrameHeight;
  
  public Runner()
  { 
    chip8 = new CHIP8(false, 12);
    
    mainFrame = new JFrame("Weston's CHIP-8 Emulator");
    mainFrame.getContentPane().add(chip8.getScreen());
    
    buildMenu();
    
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    centerFrame();
                          
    keyListener = new KeyAdapter(){
      public void keyPressed(KeyEvent e)
      {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
          JMenuBar oldMenu = mainFrame.getJMenuBar();
          if(oldMenu != null)
            mainFrame.setJMenuBar(null);
          else
            mainFrame.setJMenuBar(menu);
          mainFrame.validate();
          centerFrame();
        }
        else if(e.getKeyCode() == KeyEvent.VK_P)
        {
          wasPaused = !chip8.getCpuCore().isPaused();
          chip8.getCpuCore().setPaused(wasPaused);
        }
      }
    };
    
    mainFrame.addFocusListener(this);
    mainFrame.addKeyListener(keyListener);
    chip8.getScreen().addKeyListener(keyListener);
    mainFrame.addKeyListener(chip8);
    mainFrame.setVisible(true);
    oldFrameWidth = mainFrame.getWidth();
    oldFrameHeight = mainFrame.getHeight();
    
    ComponentListener compListener = new ComponentAdapter()
    { 
      public void componentResized(ComponentEvent e)
      {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if(mainFrame.getWidth() > screenSize.getWidth())
        {
          mainFrame.setSize((int)screenSize.getWidth(), mainFrame.getHeight());
          mainFrame.setLocation(0, (int)screenSize.getHeight()/2-mainFrame.getHeight()/2);
        }
        if(mainFrame.getHeight() != mainFrame.getWidth()/2)
        {
          if(Math.abs(oldFrameWidth-mainFrame.getWidth()) > Math.abs(oldFrameHeight-mainFrame.getHeight()))
          {
            mainFrame.setSize(mainFrame.getWidth(), mainFrame.getWidth()/2);
          }
          else
          {
            mainFrame.setSize(mainFrame.getHeight()*2, mainFrame.getHeight());
          }
        }
        oldFrameWidth = mainFrame.getWidth();
        oldFrameHeight = mainFrame.getHeight();
      }
    };
    mainFrame.addComponentListener(compListener);
  }
  
  private void buildMenu()
  {
    menu = new JMenuBar();
    
    file = new JMenu("File");
    file.setMnemonic('F');
    file.addActionListener(this);
    options = new JMenu("Options");
    options.addActionListener(this);
    options.setMnemonic('O');
    exit = new JMenuItem("Exit", 'X');
    exit.addActionListener(this);
    exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
    load = new JMenuItem("Load", 'L');
    load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK));
    load.addActionListener(this);
    reset = new JMenuItem("Reset", 'R');
    reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK));
    reset.addActionListener(this);
    sound = new JCheckBoxMenuItem("Sound", true);
    sound.setMnemonic('S');
    sound.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
    sound.addActionListener(this);
    reduceFlicker = new JCheckBoxMenuItem("Reduce Flicker", true);
    reduceFlicker.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
    reduceFlicker.setMnemonic('F');
    reduceFlicker.addActionListener(this);
    colorFading = new JCheckBoxMenuItem("Color Fading", true);
    colorFading.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
    colorFading.setMnemonic('C');
    colorFading.addActionListener(this);
    plainTiles = new JCheckBoxMenuItem("Plain Tiles", false);
    plainTiles.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK));
    plainTiles.setMnemonic('T');
    plainTiles.addActionListener(this);
    file.add(load);
    file.add(reset);
    file.add(new JSeparator(JSeparator.HORIZONTAL));
    file.add(exit);
    options.add(sound);
    options.add(new JSeparator(JSeparator.HORIZONTAL));
    options.add(reduceFlicker);
    options.add(colorFading);
    options.add(plainTiles);
    menu.add(file);
    menu.add(options);
    mainFrame.setJMenuBar(menu);
    mainFrame.getContentPane().setBackground(Color.BLACK);
  }
  
  private void centerFrame()
  {
    mainFrame.pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int width = (int)screenSize.getWidth();
    int height = (int)screenSize.getHeight();
    mainFrame.setLocation(width/2-mainFrame.getWidth()/2,
                          height/2-mainFrame.getHeight()/2);
  }
  
  public void focusLost(FocusEvent e)
  {
    Object source = e.getSource();
    oldWidth = chip8.getScreen().getWidth();
    oldHeight = chip8.getScreen().getHeight();
    chip8.getCpuCore().setPaused(true);
    mainFrame.remove(chip8.getScreen());
    canvasRemoved = true;
  }
  
  public void focusGained(FocusEvent e)
  {
    Object source = e.getSource();
    if(!wasPaused)
      chip8.getCpuCore().setPaused(false);
    if(canvasRemoved)
    {
      TextureLoader.clear();
      chip8.recreateScreen();
      chip8.getScreen().addKeyListener(keyListener);
      mainFrame.getContentPane().add(chip8.getScreen());
      chip8.getScreen().setSize(oldWidth, oldHeight);
      canvasRemoved = false;
      
      chip8.getScreen().reload();
    }
  }
  
  public void actionPerformed(ActionEvent e)
  {
    Object source = e.getSource();
    if(e.getSource() == exit)
      System.exit(0);
    else if(e.getSource() == reset)
    {
      chip8.reset();
      chip8.loadLastRom();
    }
    else if(e.getSource() == load)
    {
      loadRom();
      chip8.getCpuCore().setPaused(false);
    }
    else if(e.getSource() == sound)
    {
      if(sound.isSelected())
        chip8.getCpuCore().setSoundEnabled(true);
      else
        chip8.getCpuCore().setSoundEnabled(false);
    }
    else if(e.getSource() == reduceFlicker)
    {
      if(reduceFlicker.isSelected())
        chip8.getScreen().setReduceFlicker(true);
      else
        chip8.getScreen().setReduceFlicker(false);
    }
    else if(e.getSource() == colorFading)
    {
      if(colorFading.isSelected())
        chip8.getScreen().setUseColorFading(true);
      else
        chip8.getScreen().setUseColorFading(false);
    }
    else if(e.getSource() == plainTiles)
    {
      if(plainTiles.isSelected())
        chip8.getScreen().setUseBlur(false);
      else
        chip8.getScreen().setUseBlur(true);
    }
  }
  
  private void loadRom()
  {
    JFileChooser chooser = new JFileChooser(new File("roms"));
    chooser.showOpenDialog(mainFrame);
    chooser.setMultiSelectionEnabled(false);
    File file = chooser.getSelectedFile();
    if(file != null)
    {
      chip8.reset();
      chip8.loadProgram(file);
    }
  }
  
  public static void main(String[] args)
  {
    new Runner();
  }
}