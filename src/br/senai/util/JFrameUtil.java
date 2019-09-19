/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.senai.util;

import java.awt.Toolkit;
import javax.swing.JFrame;

/**
 *
 * @author Igor Lobo
 */
public class JFrameUtil {
    
    private static JFrameUtil instancia;
    
    private JFrameUtil(){
        
    }
    
    public static JFrameUtil getInstance(){
        if (instancia == null) instancia = new JFrameUtil();
        return instancia;
    }
    
    public void setIcon(JFrame frame){
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(Constants.IMAGE_PATH_LOGO)));
    }
    
}
