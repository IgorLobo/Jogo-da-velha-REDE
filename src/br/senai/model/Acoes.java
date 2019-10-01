/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.senai.model;

import br.senai.application.telaJogo;
import br.senai.util.ConexaoTCP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author Hygor
 */
public class Acoes {

    private telaJogo mainFrame;
    private GameConfig gameConfig = GameConfig.getInstance();

    public Acoes() {
    }

    public Acoes(telaJogo mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void acaoPadraoId(ProtocoloComunicacao pc, String hostAddress, int port) throws IOException {
        String[] mensagem;
        switch (pc.getId()) {
            case "01":
                this.enviarUDP(hostAddress, 20192, pc.enviarMensagem("02" + gameConfig.getJogador1().getApelido()));
                mainFrame.insereJogadorOnline(new Jogador(pc.getMensagem(), hostAddress, port));
                mainFrame.atualizar();
                break;
            case "02":
                mainFrame.insereJogadorOnline(new Jogador(pc.getMensagem(), hostAddress, port));
                mainFrame.atualizar();
                break;
            case "03":
                mainFrame.removerJogadorOnline(pc.getMensagem(), hostAddress, port);
                mainFrame.atualizar();
                break;
            case "04":
                mainFrame.mostraMensagem("R", hostAddress + ":" + port + " - " + pc.getMensagem(), "está te convidando para uma partida.");
                break;
            case "05":
                mensagem = pc.getMensagem().split(";");
                Jogador jogador;
                if (Integer.parseInt(mensagem[1]) != 0) {
                    jogador = mainFrame.obterJogador(mensagem[0], hostAddress, port);
                    if (jogador != null) {
                        mainFrame.mostraMensagem("A", hostAddress + ":" + port + " - " + mensagem[0], "está te esperando na porta "+mensagem[1]+".");
                        enviarUDP(hostAddress, 20192, pc.enviarMensagem("06Ok"));
                    } else {
                        mainFrame.mostraMensagem("A", hostAddress + ":" + port + " - " + mensagem[0], "não é um jogador válido.");
                    }
                }
                break;
            case "06":
                if (pc.getMensagem().substring(pc.getMensagem().length()-2).equalsIgnoreCase("Ok")) {
                    mainFrame.mostraMensagem("A", hostAddress + ":" + port, "está te esperando na porta informada.");
                }
                break;
            case "07":

                break;
            case "08":

                break;
            case "09":

                break;
            default:
                break;
        }
    }

    private void enviarUDP(String ipDestino, int portaDestino, String msg) {
        try {
            // cria endereço para enviar mensagem
            InetAddress addr = InetAddress.getByName(ipDestino);

            // cria pacote de dados para ser enviado
            DatagramPacket p = new DatagramPacket(msg.getBytes(),
                    msg.getBytes().length,
                    addr, portaDestino);

            // obtem endereço da interface de rede selecionada
            InetAddress addrLocal = mainFrame.obtemInterfaceRede();
            if (addrLocal == null) {
                JOptionPane.showMessageDialog(null,
                        "Erro na obtenção da interface escolhida.",
                        "Envia/Recebe mensagens via UDP",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // cria um socket do tipo datagram e
            // liga-o a qualquer porta disponível
            DatagramSocket udpSocket = new DatagramSocket(0, addrLocal);

            // envia pacote para o endereço e porta especificados
            udpSocket.send(p);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Erro no envio da mensagem.\n Erro: " + ex.getMessage(),
                    "Envia/Recebe mensagens via UDP",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
