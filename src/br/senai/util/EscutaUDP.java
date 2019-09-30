/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.senai.util;

import br.senai.application.telaJogo;
import br.senai.model.Acoes;
import br.senai.model.ProtocoloComunicacao;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.SwingWorker;

/**
 *
 * @author Hygor
 */
public class EscutaUDP extends SwingWorker<Void, String> {

    private telaJogo mainFrame; // frame principal do programa
    private DatagramSocket udpSocket;
    private int porta;
    private InetAddress addrLocal;

    public EscutaUDP(telaJogo mainFrame, int porta,
            InetAddress addr) throws SocketException {
        this.mainFrame = mainFrame;
        this.porta = porta;

        this.addrLocal = addr;
        udpSocket = new DatagramSocket(porta, addr);
        udpSocket.setReuseAddress(true);
    }

    @Override
    protected Void doInBackground() throws Exception {
        // escuta porta
        String msg;
        ProtocoloComunicacao protocolo = new ProtocoloComunicacao();
        Acoes acoes = new Acoes(mainFrame);
        while (true) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            // bloqueia até que um pacote seja lido
            try {
                udpSocket.receive(packet);
            } catch (IOException ex) {
                mainFrame.mostraMensagemRecebida(
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), ex.getMessage());
                continue;
            }

            // obtém dados
            msg = new String(packet.getData()).trim();
            protocolo.verificarMensagemRecebida(msg);
            // mostra mensagem recebida
            mainFrame.mostraMensagemRecebida(
                    packet.getAddress().getHostAddress(),
                    packet.getPort(), msg);
            acoes.acaoPadraoId(protocolo, packet.getAddress().getHostAddress(),  packet.getPort());
        }
    }

    public void encerraConexao() {
        if (udpSocket.isConnected()) {
            udpSocket.disconnect();
        }

        udpSocket.close();
    }
}
