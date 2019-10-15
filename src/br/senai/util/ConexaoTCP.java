/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.senai.util;

import br.senai.application.telaJogo;
import br.senai.model.Acoes;
import br.senai.model.Jogador;
import br.senai.model.ProtocoloComunicacao;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.swing.SwingWorker;

/**
 *
 * @author Hygor
 */
public class ConexaoTCP extends SwingWorker<Boolean, String> {

    private final telaJogo mainFrame;
    private final Socket socket;
// leitura dos dados
    private InputStream entrada;
    private InputStreamReader inr;
    private BufferedReader bfr;
// envio dos dados
    private OutputStream saida;
    private OutputStreamWriter outw;
    private BufferedWriter bfw;

    public Socket getSocket() {
        return socket;
    }
    
    public ConexaoTCP(telaJogo mainFrame, Socket socket) {
        this.mainFrame = mainFrame;
        this.socket = socket;
        try {
            entrada = this.socket.getInputStream();
            inr = new InputStreamReader(entrada, "ISO-8859-1");
            bfr = new BufferedReader(inr);
            saida = this.socket.getOutputStream();
            outw = new OutputStreamWriter(saida, "ISO-8859-1");
            bfw = new BufferedWriter(outw);
        } catch (IOException e) {
            mainFrame.mostraMensagem("",
                    socket.getRemoteSocketAddress().toString(),
                    "Erro: criação da nova conexão");
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        String msg;
        ProtocoloComunicacao protocolo = new ProtocoloComunicacao();
        Acoes acoes = new Acoes(mainFrame);
        while (true) {
            try {
                msg = (String) bfr.readLine();
                if (msg != null) {
                    String addr
                            = socket.getRemoteSocketAddress().toString();

                    // elimina '/' no endereço
                    addr = addr.replace("/", "");
                    
                    protocolo.verificarMensagemRecebida(msg);
                    // mostra mensagem recebida
                    mainFrame.mostraMensagemRecebida(addr, msg);
                    acoes.acaoPadraoId(protocolo, addr, socket.getPort());

                } else {
                    // encerra atributos de comunicação
                    bfr.close();
                    inr.close();
                    entrada.close();
                    bfw.close();
                    outw.close();
                    saida.close();
                    socket.close();

                    mainFrame.encerraConexao(this, false);
                    Thread.currentThread().stop();
                }
            } catch (IOException ex) {
                // mostra mensagem de erro
                mainFrame.mostraMensagemRecebida(
                        socket.getRemoteSocketAddress().toString(),
                        ex.getMessage());
                return false;
            }
        }
    }

    public boolean enviarMensagemTCP(String msg) {
        try {
            outw.write(msg + "\n");
            outw.flush();
            String addr = socket.getRemoteSocketAddress().toString();
            // elimina '/' no endereço
            addr = addr.replace("/", "");
            // mostra mensagem enviada
            mainFrame.mostraMensagemEnviada(addr, msg);
            return true;
        } catch (IOException ex) {
            mainFrame.mostraMensagemEnviada(
                    socket.getRemoteSocketAddress().toString(),
                    "Erro: envio da mensagem [" + msg + "]");
            return false;
        }
    }

    public void encerraConexao() {
        try {
            socket.close();
        } catch (IOException ex) {
        }
    }

    public BufferedReader getBfr() {
        return bfr;
    }

    public void setBfr(BufferedReader bfr) {
        this.bfr = bfr;
    }
}
