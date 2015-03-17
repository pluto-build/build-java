package org.sugarj.cleardep.buildjava;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.sugarj.cleardep.build.SomeClass;
import org.sugarj.common.Log;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.util.HotSwapper;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class HotSwapTest {
  public static void main(String[] args) {  
    try {
      HotSwapper hs = new HotSwapper(8000);

      System.out.println(new SomeClass().toString());
      
      do {
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.get("org.sugarj.cleardep.build.SomeClass");
        byte[] byteCode = cc.toBytecode();//loadClassData("org.sugarj.cleardep.build.SomeClass");
        hs.reload("org.sugarj.cleardep.build.SomeClass", byteCode);
        
        System.out.println(new SomeClass().toString());
        
        System.in.read();
      } while (true);
    } catch (IOException | IllegalConnectorArgumentsException e) {
      Log.log.log("Hotswap unsuccessful. Please start the instance with the jvm parameters -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000", Log.CORE);
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CannotCompileException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static byte[] loadClassData(String className) throws IOException {
    /*URL url = cl.getResource(className.substring(className.lastIndexOf(".")+1) + ".class");
    URL resFile = FileLocator.resolve(url);*/
    
    File f = new File("bin/" + className.replaceAll("\\.", "/") + ".class");
    int size = (int) f.length();
    byte buff[] = new byte[size];
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream(fis);
    dis.readFully(buff);
    dis.close();
    return buff;
  }
}
