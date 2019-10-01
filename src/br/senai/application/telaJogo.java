package br.senai.application;

import br.senai.model.GameConfig;
import br.senai.model.Jogador;
import br.senai.model.Simbolo;
import br.senai.util.ConexaoTCP;
import br.senai.util.EscutaTCP;
import br.senai.util.EscutaUDP;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.VK_NUMPAD1;
import static java.awt.event.KeyEvent.VK_NUMPAD2;
import static java.awt.event.KeyEvent.VK_NUMPAD3;
import static java.awt.event.KeyEvent.VK_NUMPAD4;
import static java.awt.event.KeyEvent.VK_NUMPAD5;
import static java.awt.event.KeyEvent.VK_NUMPAD6;
import static java.awt.event.KeyEvent.VK_NUMPAD7;
import static java.awt.event.KeyEvent.VK_NUMPAD8;
import static java.awt.event.KeyEvent.VK_NUMPAD9;
import static java.awt.event.KeyEvent.VK_SPACE;
import javax.swing.JOptionPane;
import br.senai.util.JFrameUtil;
import java.awt.Container;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;

/**
 * @author Hygor
 * @author Igor
 */
public class telaJogo extends javax.swing.JFrame {

    private final int MAX_CONEXOES = 10;
    private ServerSocket servidorTCP;
    // thread de escuta da porta TCP
    private EscutaTCP tcpEscutaThread;
    // indica se está escutando porta, eeperando conexão
    private boolean estaEscutando;
    // indica de programa conectou a um determinado servidor
    private boolean estaConectado;
    // lista com as conexões TCP ativas
    private ArrayList<ConexaoTCP> lstConexoes;
    private Set<Jogador> lstJogadoresOnline;
    private EscutaUDP udpEscutaThread; // thread para leitura da porta UDP
    private int rodadas = 1;
    private int jogadas = 0;
    private boolean ganhador = false;
    private Simbolo simboloDaVez = null;
    private boolean statusJogo = false;

    public telaJogo() {
        initComponents();
        iniciarCampos();
        JFrameUtil.getInstance().setIcon(this);
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!telaJogo.this.isVisible()) {
                    return;
                }
                try {
                    KeyEvent evt = (KeyEvent) event;
                    if (evt.getID() == KeyEvent.KEY_PRESSED) {
                        switch (evt.getKeyCode()) {
                            case VK_NUMPAD1:
                                M1.doClick();
                                break;
                            case VK_NUMPAD2:
                                M2.doClick();
                                break;
                            case VK_NUMPAD3:
                                M3.doClick();
                                break;
                            case VK_NUMPAD4:
                                M4.doClick();
                                break;
                            case VK_NUMPAD5:
                                M5.doClick();
                                break;
                            case VK_NUMPAD6:
                                M6.doClick();
                                break;
                            case VK_NUMPAD7:
                                M7.doClick();
                                break;
                            case VK_NUMPAD8:
                                M8.doClick();
                                break;
                            case VK_NUMPAD9:
                                M9.doClick();
                                break;
                            case VK_SPACE:
                                evt.consume();
                                break;
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    public void iniciarCampos() {

        JL_J1.setText(GameConfig.getInstance().getJogador1().getApelido() + ":");
        JL_J2.setText(GameConfig.getInstance().getJogador2().getApelido() + ":");
        simboloDaVez = GameConfig.getInstance().getJogador1().getSimbolo();

        JL_totalRodadas.setText(Integer.toString(GameConfig.getInstance().getRodadas()));
        atualizar();
        JL_vez.setText(GameConfig.getInstance().getJogador1().getApelido());
        btn_NovoJogo.setToolTipText("Cria um novo jogo! :)");
        Dica.setToolTipText("<html><b>Dica:</b> Você pode jogar pelo teclado numérico também!<html>");

        // Coletar e mostrar interfaces de rede cadastradas
        try {
            Enumeration<NetworkInterface> nets
                    = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                // descarta interfaces virtuais e loopback (127.0.0.1)
                if (netint.isVirtual() || netint.isLoopback()) {
                    continue;
                }

                // endereços associados à interface
                Enumeration<InetAddress> inetAddresses
                        = netint.getInetAddresses();
                if (inetAddresses.hasMoreElements()) {
                    for (InetAddress inetAddress
                            : Collections.list(inetAddresses)) {
                        if ((inetAddress instanceof Inet4Address)
                                && inetAddress.isSiteLocalAddress()) {
                            interfacesJCombo.addItem(
                                    inetAddress.getHostAddress()
                                    + " - " + netint.getDisplayName());
                        }
                    }
                }
            }
        } catch (SocketException ex) {
        }
        tcpEscutaThread = null;
        servidorTCP = null;
        estaEscutando = false;
        DefaultListModel<String> model = new DefaultListModel<String>();
        lstMensagens.setModel(model);
        lstConexoes = new ArrayList<ConexaoTCP>();
        lstJogadoresOnline = new HashSet<Jogador>();
    }

    public void trocarVez() {//******PASSA A VEZ DOS JOGADORES****************
        testarGanhador(simboloDaVez);
        if (simboloDaVez == Simbolo.X) {
            simboloDaVez = Simbolo.O;
        } else {
            simboloDaVez = Simbolo.X;
        }
        if (jogadas == 9 && ganhador == false) {
            M1.setForeground(Color.red);
            M2.setForeground(Color.red);
            M3.setForeground(Color.red);
            M4.setForeground(Color.red);
            M5.setForeground(Color.red);
            M6.setForeground(Color.red);
            M7.setForeground(Color.red);
            M8.setForeground(Color.red);
            M9.setForeground(Color.red);
            JOptionPane.showMessageDialog(null, "Deu velha :(");
            mostrarGanhador(null);
            limpar();
        }

        atualizar();
    }

    public void testarGanhador(Simbolo simbolo) {//******Teste ganhador***********
        //*******Teste linhas**************

        if (M1.getText().equals(simbolo.simbolo) && M2.getText().equals(simbolo.simbolo) && M3.getText().equals(simbolo.simbolo)) {
            M1.setForeground(Color.green);
            M2.setForeground(Color.green);
            M3.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M4.getText().equals(simbolo.simbolo) && M5.getText().equals(simbolo.simbolo) && M6.getText().equals(simbolo.simbolo)) {
            M4.setForeground(Color.green);
            M5.setForeground(Color.green);
            M6.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M7.getText().equals(simbolo.simbolo) && M8.getText().equals(simbolo.simbolo) && M9.getText().equals(simbolo.simbolo)) {
            M7.setForeground(Color.green);
            M8.setForeground(Color.green);
            M9.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M7.getText().equals(simbolo.simbolo) && M4.getText().equals(simbolo.simbolo) && M1.getText().equals(simbolo.simbolo)) {
            M7.setForeground(Color.green);
            M4.setForeground(Color.green);
            M1.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M8.getText().equals(simbolo.simbolo) && M5.getText().equals(simbolo.simbolo) && M2.getText().equals(simbolo.simbolo)) {
            M8.setForeground(Color.green);
            M5.setForeground(Color.green);
            M2.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M9.getText().equals(simbolo.simbolo) && M6.getText().equals(simbolo.simbolo) && M3.getText().equals(simbolo.simbolo)) {
            M9.setForeground(Color.green);
            M6.setForeground(Color.green);
            M3.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M7.getText().equals(simbolo.simbolo) && M5.getText().equals(simbolo.simbolo) && M3.getText().equals(simbolo.simbolo)) {
            M7.setForeground(Color.green);
            M5.setForeground(Color.green);
            M3.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();
        } else if (M1.getText().equals(simbolo.simbolo) && M5.getText().equals(simbolo.simbolo) && M9.getText().equals(simbolo.simbolo)) {
            M1.setForeground(Color.green);
            M5.setForeground(Color.green);
            M9.setForeground(Color.green);
            mostrarGanhador(simbolo);
            limpar();

        }

    }

    public void mostrarGanhador(Simbolo simbolo) {//**********************
        Jogador jogadorVencedor = GameConfig.getInstance().getJogador(simbolo);
        ganhador = true;
        if (jogadorVencedor != null) {
            simboloDaVez = jogadorVencedor.getSimbolo();
            jogadorVencedor.adicionarPonto(5);
            jogadorVencedor.adicionarVitoria(1);
            atualizar();
            JOptionPane.showMessageDialog(null, "Parabéns, " + jogadorVencedor.getApelido() + " ganhou a rodada!!");

        } else {
            GameConfig.getInstance().getJogador1().adicionarPonto(2);
            GameConfig.getInstance().getJogador2().adicionarPonto(2);

            JL_pontosJ1.setText(Integer.toString(GameConfig.getInstance().getJogador1().getPontos()));
            JL_pontosJ2.setText(Integer.toString(GameConfig.getInstance().getJogador2().getPontos()));
            JL_Empates.setText(Integer.toString(Integer.parseInt(JL_Empates.getText()) + 1));
        }
    }

    public void limpar() {
        jogadas = 0;
        if (rodadas < GameConfig.getInstance().getRodadas()) {
            M1.setText("");
            M2.setText("");
            M3.setText("");
            M4.setText("");
            M5.setText("");
            M6.setText("");
            M7.setText("");
            M8.setText("");
            M9.setText("");

            rodadas++;
            ganhador = false;
        } else {
            M1.setEnabled(false);
            M2.setEnabled(false);
            M3.setEnabled(false);
            M4.setEnabled(false);
            M5.setEnabled(false);
            M6.setEnabled(false);
            M7.setEnabled(false);
            M8.setEnabled(false);
            M9.setEnabled(false);

            Jogador jogadorGanhador = null;
            if (GameConfig.getInstance().getJogador1().getPontos() > GameConfig.getInstance().getJogador2().getPontos()) {
                jogadorGanhador = GameConfig.getInstance().getJogador1();
            } else if (GameConfig.getInstance().getJogador2().getPontos() > GameConfig.getInstance().getJogador1().getPontos()) {
                jogadorGanhador = GameConfig.getInstance().getJogador2();
            }

            if (jogadorGanhador != null) {
                JOptionPane.showMessageDialog(null, "Parabéns " + jogadorGanhador.getApelido() + ",  você ganhou!!");
            } else {
                JOptionPane.showMessageDialog(null, "Empate!!");
            }

        }
        M1.setForeground(Color.black);
        M2.setForeground(Color.black);
        M3.setForeground(Color.black);
        M4.setForeground(Color.black);
        M5.setForeground(Color.black);
        M6.setForeground(Color.black);
        M7.setForeground(Color.black);
        M8.setForeground(Color.black);
        M9.setForeground(Color.black);
    }

    public void atualizar() {
        JL_jogadas.setText(Integer.toString(jogadas));
        JL_Rodada.setText(Integer.toString(rodadas));

        JL_pontosJ1.setText(Integer.toString(GameConfig.getInstance().getJogador1().getPontos()));
        JL_vitoriasJ1.setText(Integer.toString(GameConfig.getInstance().getJogador1().getVitorias()));
        JL_pontosJ2.setText(Integer.toString(GameConfig.getInstance().getJogador2().getPontos()));
        JL_vitoriasJ2.setText(Integer.toString(GameConfig.getInstance().getJogador2().getVitorias()));

        Jogador jogadorDaVez = GameConfig.getInstance().getJogador(simboloDaVez);
        JL_vez.setText(jogadorDaVez.getApelido());
        JL_vezSimbolo.setText(jogadorDaVez.getSimbolo().simbolo);

    }

    private void encerraPrograma() {
        // encerra o programa
        Container frame = btn_Encerrar.getParent();
        do {
            frame = frame.getParent();
        } while (!(frame instanceof JFrame));
        ((JFrame) frame).dispose();
    }

    public void encerraConexao(ConexaoTCP conex, boolean servidor) {
        conex.encerraConexao();
        // elimina conexão da lista de conexões
        int idx;
        if (servidor) {
            idx = encontraConexao(true);
        } else {
            idx = conexoesJCombo.getSelectedIndex();
        }
        if (idx >= 0) {
            conexoesJCombo.remove(idx);
        }
    }

    private void escutaPortaUDP() {
        // verifica se usuário indicou a porta
        // verifica se usuário forneceu uma porta válida
        int porta = 0;
        if (tryParseInt(txtRecebePorta.getText())) {
            porta = Integer.parseInt(txtRecebePorta.getText());
        }

        if (porta < 1024) {
            txtRecebePorta.requestFocus();
            return;
        }

        // verifica se usuário escolheu a interface
        int nInterface = interfacesJCombo.getSelectedIndex();
        if (nInterface < 0) {
            interfacesJCombo.requestFocus();
            return;
        }

        // obtem endereço da interface de rede selecionada
        InetAddress addrLocal = obtemInterfaceRede();
        if (addrLocal == null) {
            JOptionPane.showMessageDialog(null,
                    "Erro na obtenção da interface escolhida.",
                    "Envia/Recebe mensagens via UDP",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // cria thread para leitura da porta UDP
        try {
            udpEscutaThread = new EscutaUDP(this, porta, addrLocal);
            mostraMensagem("", "", 0,
                    "Escutando porta " + txtRecebePorta.getText());
        } catch (SocketException ex) {
            JOptionPane.showMessageDialog(null,
                    "Erro na criação do thread de leitura da porta " + porta
                    + ".\n" + ex.getMessage(),
                    "Envia/Recebe mensagens via UDP",
                    JOptionPane.ERROR_MESSAGE);
            encerraPrograma();
            return;
        }

        // habilita/desabilita controles
        btnEscutar.setText("Parar");
        txtRecebePorta.setEnabled(false);
        estaEscutando = true;
        interfacesJCombo.setEnabled(false);

        // executa thread de leitura da porta UDP
        udpEscutaThread.execute();
    }

    private void escutaPortaTCP() {
        // verifica se usuário indicou a porta
        // verifica se usuário forneceu uma porta válida
        int porta = 0;
        if (tryParseInt(txtRecebePorta.getText())) {
            porta = Integer.parseInt(txtRecebePorta.getText());
        }
        if (porta < 1024) {
            txtRecebePorta.requestFocus();
            return;
        }
        // verifica se usuário escolheu a interface
        int nInterface = interfacesJCombo.getSelectedIndex();
        if (nInterface < 0) {
            interfacesJCombo.requestFocus();
            return;
        }
        // obtem endereço da interface de rede selecionada
        InetAddress addrLocal = obtemInterfaceRede();
        if (addrLocal == null) {
            JOptionPane.showMessageDialog(null,
                    "Erro na obtenção da interface escolhida.",
                    "Envia/Recebe mensagens via TCP",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // cria servidor TCP
        servidorTCP = criarSocketTCP(porta);
        if (servidorTCP == null) {
            JOptionPane.showMessageDialog(null,
                    "Erro na criação da conexão TCP.",
                    "Envia/Recebe mensagens via TCP",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // cria thread para leitura da porta TCP
        tcpEscutaThread = new EscutaTCP(this, servidorTCP, addrLocal);
        mostraMensagem("", "", "Escutando porta "
                + txtRecebePorta.getText());
        // habilita/desabilita controles
        btnEscutar.setText("Parar");
        txtRecebePorta.setEnabled(false);
        estaEscutando = true;
        // executa thread de leitura da porta TCP
        tcpEscutaThread.execute();
    }

    public void mostraMensagemRecebida(String endereco, int porta, String conteudo) {
        mostraMensagem("R", endereco, porta, conteudo);
    }

    public void mostraMensagem(String prefixo, String endereco, int porta, String conteudo) {
        String msg;
        if (prefixo.isEmpty()) {
            msg = "";
        } else {
            msg = prefixo + " [" + endereco + ":"
                    + (porta > 0 ? String.valueOf(porta) : "") + "] ";
        }

        msg += conteudo;

        ((DefaultListModel) lstMensagens.getModel()).addElement(msg);
    }

    public void mostraMensagemRecebida(String endereco, String conteudo) {
        mostraMensagem("R", endereco, conteudo);
    }

    public void mostraMensagemEnviada(String endereco, String conteudo) {
        mostraMensagem("E", endereco, conteudo);
    }

    public void mostraMensagem(String prefixo, String endereco, String conteudo) {
        String msg;
        if (prefixo.isEmpty()) {
            msg = conteudo;
        } else {
            msg = prefixo + " [" + endereco + "] " + conteudo;
        }
        ((DefaultListModel) lstMensagens.getModel()).addElement(msg);
    }

    private void conectaComServidor() {
        // verifica se IP foi fornecido
        if (txtConectaIP.getText().isEmpty()) {
            txtConectaIP.requestFocus();
            return;
        }
        int porta;
        try {
            // número fornecido pelo usuário
            porta = Integer.parseInt(txtConectaPorta.getText());
            if (porta < 1) {
                txtConectaPorta.requestFocus();
                return;
            }
        } catch (NumberFormatException ex) {
            // mostra mensagem de erro
            JOptionPane.showMessageDialog(this,
                    "Número da porta deve ser um número inteiro",
                    "Iniciar Conexão", JOptionPane.ERROR_MESSAGE);

            txtConectaPorta.requestFocus();
            return;
        }
        try {
            Socket skt = new Socket(txtConectaIP.getText(), porta);
            String addr = txtConectaIP.getText() + ":"
                    + txtConectaPorta.getText();
            mostraMensagem("", addr, "Conectado com servidor TCP");
            // cria nova thread para tratar a conexão
            ConexaoTCP novaConexao = new ConexaoTCP(this, skt);
            novaConexao.execute();
            // atualiza lista de conexões ativas
            insereConexao(novaConexao);
            // atualiza controlespaint
            estaConectado = true;
            txtConectaIP.setEnabled(false);
            txtConectaPorta.setEnabled(false);
            btnConectar.setText("Desconectar");
        } catch (IOException ex) {
            String msg = "Erro: " + ex.getMessage();
            JOptionPane.showMessageDialog(this, msg,
                    "Conecta com servidor",
                    JOptionPane.ERROR_MESSAGE);
            estaConectado = false;
            txtConectaIP.setEnabled(true);
            txtConectaPorta.setEnabled(true);
            btnConectar.setText("Conectar");
        }
    }

    // procura por conexão selecionada para enviar mensagem
    private ConexaoTCP conexaoSelecionada(boolean servidor) {
        int i = encontraConexao(servidor);
        if (i < 0) {
            return null;
        }

        return lstConexoes.get(i);
    }

    private int encontraConexao(boolean servidor) {
        String addrSelecionado;
        if (servidor) {
            addrSelecionado = txtConectaIP.getText() + ":"
                    + txtConectaPorta.getText();
        } else {
            int idx = conexoesJCombo.getSelectedIndex();
            if (idx < 0) {
                return -1;
            }
            addrSelecionado = conexoesJCombo.getItemAt(idx);
        }
        String addr;
        for (int i = 0; i < lstConexoes.size(); ++i) {
            addr = lstConexoes.get(i).getSocket().
                    getRemoteSocketAddress().toString();
            // elimina '/' no endereço
            addr = addr.replace("/", "");
            if (addr.compareTo(addrSelecionado) == 0) {
                return i;
            }
        }
        return -1;
    }

    public InetAddress obtemInterfaceRede() {
        // verifica se usuário escolheu a interface
        int nInterface = interfacesJCombo.getSelectedIndex();
        if (nInterface < 0) {
            return null;
        }

        // obtem interface selecionada pelo usuário
        String str = interfacesJCombo.getItemAt(nInterface);
        String[] strParts = str.split(" - ");
        InetAddress addr;
        try {
            addr = InetAddress.getByName(strParts[0]);
        } catch (UnknownHostException ex) {
            return null;
        }

        return addr;
    }

    // cria e abre um socket TCP em uma porta qualquer
    // na interface indicada
    private ServerSocket criarSocketTCP(int porta) {
        InetAddress addr = obtemInterfaceRede();
        if (addr == null) {
            return null;
        }
        ServerSocket socket;
        try {
            // cria um socket para servidor TCP.
            // Parâmetros:
            // porta: se 0, usar uma porta que será
            // alocada automaticamente
            // backlog: número máximo de conexões aceitas
            // bindAddr: addr (InetAddress local que o servidor
            // irá ligar)
            socket = new ServerSocket(porta, MAX_CONEXOES, addr);
            socket.setReuseAddress(true);
        } catch (IOException e) {
            return null;
        }
        return socket;
    }

    public void insereConexao(ConexaoTCP novaConexao) {
        // adiciona nova conexão na lista de conexões ativas
        lstConexoes.add(novaConexao);
        // mostra nova conexão, sem '/' no endereço
        SocketAddress remoteAddr
                = novaConexao.getSocket().getRemoteSocketAddress();
        conexoesJCombo.addItem(
                remoteAddr.toString().replace("/", ""));
    }

    public void insereJogadorOnline(Jogador jogadorNovo) {
        // adiciona nova conexão na lista de conexões ativas
        lstJogadoresOnline.removeIf(jogador -> (jogador.getApelido().equalsIgnoreCase(jogadorNovo.getApelido()) && jogador.getIp().equalsIgnoreCase(jogadorNovo.getIp()) && jogador.getPort() == jogadorNovo.getPort()));
        lstJogadoresOnline.add(jogadorNovo);
        // mostra nova conexão, sem '/' no endereço
        jogadoresOnlineJCombo.addItem(
                jogadorNovo.getIp().replace("/", "") + ":" + jogadorNovo.getPort() + " - " + jogadorNovo.getApelido());
    }

    public void removerJogadorOnline(String Apelido, String ip, int porta) {
        lstJogadoresOnline.removeIf(jogador -> (jogador.getApelido().equalsIgnoreCase(Apelido) && jogador.getIp().equalsIgnoreCase(ip) && jogador.getPort() == porta));
        jogadoresOnlineJCombo.removeAllItems();
        lstJogadoresOnline.forEach((jogador) -> {
            jogadoresOnlineJCombo.addItem(jogador.getIp().replace("/", "") + ":" + jogador.getPort() + " - " + jogador.getApelido());
        });
    }

    private boolean tryParseInt(String valor) {
        try {
            Integer.parseInt(valor);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void enviarUDP(String msg) {
        if (txtDestino.getText().length() == 0) {
            txtDestino.requestFocus();
            return;
        }

        int porta = 0;
        if (tryParseInt(txtEnviaPorta.getText())) {
            porta = Integer.parseInt(txtEnviaPorta.getText());
        }

        if (porta < 1024) {
            txtEnviaPorta.requestFocus();
            return;
        }

        try {
            // cria endereço para enviar mensagem
            InetAddress addr = InetAddress.getByName(txtDestino.getText());

            // cria pacote de dados para ser enviado
            DatagramPacket p = new DatagramPacket(msg.getBytes(),
                    msg.getBytes().length,
                    addr, porta);

            // obtem endereço da interface de rede selecionada
            InetAddress addrLocal = obtemInterfaceRede();
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

            // mostra mensagem enviada
            mostraMensagem("E", txtDestino.getText(), porta, msg);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Erro no envio da mensagem.\n Erro: " + ex.getMessage(),
                    "Envia/Recebe mensagens via UDP",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarTCP(String msg) {
        // verifica se existe uma conexão selecionada
        ConexaoTCP conex = conexaoSelecionada(false);
        if (conex == null) {
            return;
        }
        // envia mensagem
        conex.enviarMensagemTCP(msg);
    }
    
    public Jogador obterJogador(String apelido, String ip, int porta){
        for (Jogador jogador : lstJogadoresOnline) {
            if (jogador.getApelido().equalsIgnoreCase(apelido) && jogador.getIp().equalsIgnoreCase(ip) && jogador.getPort() == porta) {
                return jogador;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jp_jogo = new javax.swing.JPanel();
        M7 = new javax.swing.JButton();
        M8 = new javax.swing.JButton();
        M9 = new javax.swing.JButton();
        M4 = new javax.swing.JButton();
        M5 = new javax.swing.JButton();
        M6 = new javax.swing.JButton();
        M1 = new javax.swing.JButton();
        M2 = new javax.swing.JButton();
        M3 = new javax.swing.JButton();
        jp_jogadores = new javax.swing.JPanel();
        JL_J1 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        JL_vitoriasJ1 = new javax.swing.JLabel();
        JL_pontosJ1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        JL_pontosJ2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        JL_vitoriasJ2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        JL_J2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        JL_jogadas = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        JL_Empates = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        JL_vezSimbolo = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        JL_Rodada = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        JL_totalRodadas = new javax.swing.JLabel();
        JL_vez = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        btn_Voltar = new javax.swing.JButton();
        btn_NovoJogo = new javax.swing.JButton();
        Dica = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        txtDestino = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtEnviaPorta = new javax.swing.JTextField();
        btnDesconectar = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        txtMensagem = new javax.swing.JTextField();
        btnEnviar = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        btnEscutar = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        txtRecebePorta = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstMensagens = new javax.swing.JList<>();
        jLabel14 = new javax.swing.JLabel();
        interfacesJCombo = new javax.swing.JComboBox<>();
        btn_Encerrar = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        btnConectar = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        txtConectaIP = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        txtConectaPorta = new javax.swing.JTextField();
        jPanel11 = new javax.swing.JPanel();
        jogadoresOnlineJCombo = new javax.swing.JComboBox<>();
        jPanel12 = new javax.swing.JPanel();
        conexoesJCombo = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Jogo da Velha");
        setResizable(false);

        jp_jogo.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jp_jogo.setMaximumSize(new java.awt.Dimension(350, 350));
        jp_jogo.setMinimumSize(new java.awt.Dimension(350, 350));
        jp_jogo.setPreferredSize(new java.awt.Dimension(351, 351));

        M7.setBackground(new java.awt.Color(255, 255, 255));
        M7.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M7ActionPerformed(evt);
            }
        });

        M8.setBackground(new java.awt.Color(255, 255, 255));
        M8.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M8ActionPerformed(evt);
            }
        });

        M9.setBackground(new java.awt.Color(255, 255, 255));
        M9.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M9ActionPerformed(evt);
            }
        });

        M4.setBackground(new java.awt.Color(255, 255, 255));
        M4.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M4ActionPerformed(evt);
            }
        });

        M5.setBackground(new java.awt.Color(255, 255, 255));
        M5.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M5ActionPerformed(evt);
            }
        });

        M6.setBackground(new java.awt.Color(255, 255, 255));
        M6.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M6ActionPerformed(evt);
            }
        });

        M1.setBackground(new java.awt.Color(255, 255, 255));
        M1.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M1ActionPerformed(evt);
            }
        });

        M2.setBackground(new java.awt.Color(255, 255, 255));
        M2.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M2ActionPerformed(evt);
            }
        });

        M3.setBackground(new java.awt.Color(255, 255, 255));
        M3.setFont(new java.awt.Font("Chiller", 1, 60)); // NOI18N
        M3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                M3ActionPerformed(evt);
            }
        });

        jp_jogadores.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        JL_J1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_J1.setText("Jogador 1:");

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/senai/images/Coin.png"))); // NOI18N
        jLabel1.setText("Pontos:");

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/senai/images/crown.png"))); // NOI18N
        jLabel2.setText("Vitórias:");

        JL_vitoriasJ1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_vitoriasJ1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JL_vitoriasJ1.setText("0");

        JL_pontosJ1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_pontosJ1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JL_pontosJ1.setText("0");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/senai/images/Coin.png"))); // NOI18N
        jLabel3.setText("Pontos:");

        JL_pontosJ2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_pontosJ2.setText("0");

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/senai/images/crown.png"))); // NOI18N
        jLabel6.setText("Vitórias:");

        JL_vitoriasJ2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_vitoriasJ2.setText("0");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        JL_J2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        JL_J2.setText("Jogador 2:");

        javax.swing.GroupLayout jp_jogadoresLayout = new javax.swing.GroupLayout(jp_jogadores);
        jp_jogadores.setLayout(jp_jogadoresLayout);
        jp_jogadoresLayout.setHorizontalGroup(
            jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_jogadoresLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jp_jogadoresLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JL_pontosJ1))
                    .addGroup(jp_jogadoresLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JL_vitoriasJ1))
                    .addComponent(JL_J1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 1, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jp_jogadoresLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JL_pontosJ2))
                    .addGroup(jp_jogadoresLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JL_vitoriasJ2))
                    .addComponent(JL_J2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_jogadoresLayout.setVerticalGroup(
            jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_jogadoresLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jp_jogadoresLayout.createSequentialGroup()
                            .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(JL_pontosJ1))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2)
                                .addComponent(JL_vitoriasJ1)))
                        .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jp_jogadoresLayout.createSequentialGroup()
                                .addComponent(JL_J2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(JL_pontosJ2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_jogadoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6)
                                    .addComponent(JL_vitoriasJ2)))
                            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(JL_J1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel5.setText("Jogadas:");

        JL_jogadas.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        JL_jogadas.setText("0");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel4.setText("Empates:");

        JL_Empates.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        JL_Empates.setText("0");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel8.setText("Quem joga agora:");

        JL_vezSimbolo.setFont(new java.awt.Font("Chiller", 1, 20)); // NOI18N
        JL_vezSimbolo.setText("X");

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel7.setText("Rodada:");

        JL_Rodada.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        JL_Rodada.setText("0");

        jLabel11.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel11.setText("/");

        JL_totalRodadas.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        JL_totalRodadas.setText("0");

        JL_vez.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        javax.swing.GroupLayout jp_jogoLayout = new javax.swing.GroupLayout(jp_jogo);
        jp_jogo.setLayout(jp_jogoLayout);
        jp_jogoLayout.setHorizontalGroup(
            jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_jogoLayout.createSequentialGroup()
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jp_jogadores, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jp_jogoLayout.createSequentialGroup()
                        .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_jogoLayout.createSequentialGroup()
                                .addComponent(M1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(M2, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(M3, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_jogoLayout.createSequentialGroup()
                                .addComponent(M7, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(M8, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(M9, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jp_jogoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jp_jogoLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(JL_vezSimbolo, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_jogoLayout.createSequentialGroup()
                                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(JL_vez, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jp_jogoLayout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(JL_jogadas)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(JL_Empates)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(JL_Rodada)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel11)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(JL_totalRodadas)))
                                .addGap(12, 12, 12))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_jogoLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(M4, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(M5, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(M6, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jp_jogoLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {M1, M2, M3, M4, M5, M6, M7, M8, M9});

        jp_jogoLayout.setVerticalGroup(
            jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_jogoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(JL_jogadas)
                    .addComponent(jLabel7)
                    .addComponent(JL_Rodada)
                    .addComponent(jLabel11)
                    .addComponent(JL_totalRodadas)
                    .addComponent(jLabel4)
                    .addComponent(JL_Empates))
                .addGap(18, 18, 18)
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(JL_vezSimbolo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JL_vez, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 51, Short.MAX_VALUE)
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(M7, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                    .addComponent(M8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(M9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(M4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(M5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(M6, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jp_jogoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(M1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(M2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(M3, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(37, 37, 37)
                .addComponent(jp_jogadores, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jp_jogoLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {M1, M2, M3, M4, M5, M6, M7, M8, M9});

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btn_Voltar.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btn_Voltar.setText("Voltar");
        btn_Voltar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        btn_Voltar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_VoltarActionPerformed(evt);
            }
        });

        btn_NovoJogo.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btn_NovoJogo.setText("Novo jogo");
        btn_NovoJogo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        btn_NovoJogo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_NovoJogoActionPerformed(evt);
            }
        });

        Dica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/senai/images/Yoda.png"))); // NOI18N
        Dica.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Enviar", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        jLabel9.setText("IP Destino:");

        jLabel10.setText("Porta:");

        btnDesconectar.setText("Desconectar");
        btnDesconectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDesconectarActionPerformed(evt);
            }
        });

        jLabel12.setText("Mensagem:");

        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtDestino, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(txtEnviaPorta, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel12)
                    .addComponent(txtMensagem))
                .addGap(10, 10, 10)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnEnviar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnDesconectar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnDesconectar, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtDestino, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtEnviaPorta, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMensagem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Receber", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        btnEscutar.setText("Iniciar");
        btnEscutar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEscutarActionPerformed(evt);
            }
        });

        jLabel13.setText("Porta:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(txtRecebePorta)
                    .addComponent(btnEscutar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtRecebePorta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEscutar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Mensagens Enviadas e Recebidas", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        lstMensagens.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(lstMensagens);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2)
        );

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel14.setText("Interface:");

        btn_Encerrar.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btn_Encerrar.setText("Encerrar");
        btn_Encerrar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        btn_Encerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_EncerrarActionPerformed(evt);
            }
        });

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Conectar", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        btnConectar.setText("Conectar");
        btnConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConectarActionPerformed(evt);
            }
        });

        jLabel20.setText("IP:");

        jLabel21.setText("Porta:");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtConectaIP)
                    .addComponent(btnConectar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtConectaPorta)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel21))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtConectaIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtConectaPorta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnConectar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Lista de players online", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jogadoresOnlineJCombo, 0, 207, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jogadoresOnlineJCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Lista de conexões", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(conexoesJCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(conexoesJCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel14)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(interfacesJCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(Dica, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(btn_Voltar, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_NovoJogo, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Encerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(interfacesJCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(Dica, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Voltar)
                    .addComponent(btn_Encerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_NovoJogo, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btn_NovoJogo, btn_Voltar});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jp_jogo, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jp_jogo, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents


    private void M7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M7ActionPerformed
        if (M7.getText().equals("") && ganhador == false) {
            jogadas++;
            M7.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M7ActionPerformed

    private void M4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M4ActionPerformed
        if (M4.getText().equals("") && ganhador == false) {
            jogadas++;
            M4.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M4ActionPerformed

    private void M1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M1ActionPerformed
        if (M1.getText().equals("") && ganhador == false) {
            jogadas++;
            M1.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M1ActionPerformed

    private void btn_VoltarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_VoltarActionPerformed
        telaConfiguracao jpConfiguracao = new telaConfiguracao();
        jpConfiguracao.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btn_VoltarActionPerformed

    private void M8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M8ActionPerformed
        if (M8.getText().equals("") && ganhador == false) {
            jogadas++;
            M8.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M8ActionPerformed

    private void M9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M9ActionPerformed
        if (M9.getText().equals("") && ganhador == false) {
            jogadas++;
            M9.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M9ActionPerformed

    private void M5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M5ActionPerformed
        if (M5.getText().equals("") && ganhador == false) {
            jogadas++;
            M5.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M5ActionPerformed

    private void M6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M6ActionPerformed
        if (M6.getText().equals("") && ganhador == false) {
            jogadas++;
            M6.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M6ActionPerformed

    private void M2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M2ActionPerformed
        if (M2.getText().equals("") && ganhador == false) {
            jogadas++;
            M2.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M2ActionPerformed

    private void M3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_M3ActionPerformed
        if (M3.getText().equals("") && ganhador == false) {
            jogadas++;
            M3.setText(simboloDaVez.simbolo);
            trocarVez();
            atualizar();
        }
    }//GEN-LAST:event_M3ActionPerformed

    private void btn_NovoJogoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_NovoJogoActionPerformed
        GameConfig.clear();
        telaConfiguracao jpConfiguracao = new telaConfiguracao();
        jpConfiguracao.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btn_NovoJogoActionPerformed

    private void btnEscutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEscutarActionPerformed
        if (estaEscutando) {
            // fecha porta UDP
            udpEscutaThread.encerraConexao();

            // encerra thread
            udpEscutaThread.cancel(true);

            // habilita/desabilita controles
            btnEscutar.setText("Iniciar");
            txtRecebePorta.setEnabled(true);
            estaEscutando = false;
            mostraMensagem("", "", 0,
                    "Escutando porta " + txtRecebePorta.getText());
        } else {
            escutaPortaUDP();
        }
    }//GEN-LAST:event_btnEscutarActionPerformed

    private void btnDesconectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDesconectarActionPerformed
        // verifica se usuário informou todos os parâmetros:
        // endereço, porta e mensagem
        String msg = txtMensagem.getText();
        if (msg.length() == 0) {
            txtEnviaPorta.requestFocus();
            return;
        }

        if (txtDestino.getText().length() == 0) {
            txtDestino.requestFocus();
            return;
        }

        int porta = 0;
        if (tryParseInt(txtEnviaPorta.getText())) {
            porta = Integer.parseInt(txtEnviaPorta.getText());
        }

        if (porta < 1024) {
            txtEnviaPorta.requestFocus();
            return;
        }

        try {
            // cria endereço para enviar mensagem
            InetAddress addr = InetAddress.getByName(txtDestino.getText());

            // cria pacote de dados para ser enviado
            DatagramPacket p = new DatagramPacket(msg.getBytes(),
                    msg.getBytes().length,
                    addr, porta);

            // obtem endereço da interface de rede selecionada
            InetAddress addrLocal = obtemInterfaceRede();
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

            // mostra mensagem enviada
            mostraMensagem("E", txtDestino.getText(), porta, msg);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Erro no envio da mensagem.\n Erro: " + ex.getMessage(),
                    "Envia/Recebe mensagens via UDP",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDesconectarActionPerformed

    private void btn_EncerrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_EncerrarActionPerformed
        encerraPrograma();
    }//GEN-LAST:event_btn_EncerrarActionPerformed

    private void btnConectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConectarActionPerformed
        if (estaConectado) {
            // desconecta com servidor
            ConexaoTCP conex = conexaoSelecionada(true);
            if (conex != null) {
                encerraConexao(conex, true);
                // atualiza controles
                estaConectado = false;
                txtConectaIP.setEnabled(true);
                txtConectaPorta.setEnabled(true);
                btnConectar.setText("Conectar");
            }
        } else {
            conectaComServidor();
        }
    }//GEN-LAST:event_btnConectarActionPerformed

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        // verifica se usuário informou todos os parâmetros:
        // endereço, porta e mensagem
        String msg = txtMensagem.getText();
        if (msg.length() == 0) {
            txtEnviaPorta.requestFocus();
            return;
        }

        enviarUDP(msg);
        //enviarTCP(msg);
    }//GEN-LAST:event_btnEnviarActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(telaJogo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(telaJogo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(telaJogo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(telaJogo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new telaJogo().setVisible(true);
            }
        });

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Dica;
    private javax.swing.JLabel JL_Empates;
    private javax.swing.JLabel JL_J1;
    private javax.swing.JLabel JL_J2;
    private javax.swing.JLabel JL_Rodada;
    private javax.swing.JLabel JL_jogadas;
    private javax.swing.JLabel JL_pontosJ1;
    private javax.swing.JLabel JL_pontosJ2;
    private javax.swing.JLabel JL_totalRodadas;
    private javax.swing.JLabel JL_vez;
    private javax.swing.JLabel JL_vezSimbolo;
    private javax.swing.JLabel JL_vitoriasJ1;
    private javax.swing.JLabel JL_vitoriasJ2;
    public javax.swing.JButton M1;
    public javax.swing.JButton M2;
    public javax.swing.JButton M3;
    public javax.swing.JButton M4;
    public javax.swing.JButton M5;
    public javax.swing.JButton M6;
    public javax.swing.JButton M7;
    public javax.swing.JButton M8;
    public javax.swing.JButton M9;
    private javax.swing.JButton btnConectar;
    private javax.swing.JButton btnDesconectar;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JButton btnEscutar;
    private javax.swing.JButton btn_Encerrar;
    private javax.swing.JButton btn_NovoJogo;
    private javax.swing.JButton btn_Voltar;
    private javax.swing.JComboBox<String> conexoesJCombo;
    private javax.swing.JComboBox<String> interfacesJCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox<String> jogadoresOnlineJCombo;
    private javax.swing.JPanel jp_jogadores;
    private javax.swing.JPanel jp_jogo;
    private javax.swing.JList<String> lstMensagens;
    private javax.swing.JTextField txtConectaIP;
    private javax.swing.JTextField txtConectaPorta;
    private javax.swing.JTextField txtDestino;
    private javax.swing.JTextField txtEnviaPorta;
    private javax.swing.JTextField txtMensagem;
    private javax.swing.JTextField txtRecebePorta;
    // End of variables declaration//GEN-END:variables
}
