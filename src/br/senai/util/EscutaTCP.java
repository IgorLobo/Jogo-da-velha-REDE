package br.senai.util;

import br.senai.application.telaJogo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.SwingWorker;

/**
 *
 * @author Hygor
 */
public class EscutaTCP extends SwingWorker<Boolean, String>{

    private final telaJogo mainFrame;
    private final ServerSocket socket;
    private final InetAddress addrRemoto;

    public EscutaTCP(telaJogo mainFrame, ServerSocket socket,
            InetAddress addrRemoto) {
        this.mainFrame = mainFrame;
        this.socket = socket;
        this.addrRemoto = addrRemoto;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        try {
            while (true) {

                // espera conexão
                Socket connection = socket.accept();
                // elimina '/' no endereço
                String addr
                        = connection.getRemoteSocketAddress().toString();
                addr = addr.replace("/", "");
                mainFrame.mostraMensagemRecebida(addr,
                        "Conexão na porta "
                        + socket.getLocalPort());
                // cria conexão com cliente
                ConexaoTCP novaConexao
                        = new ConexaoTCP(mainFrame, connection);
                // processa as comunicações com o cliente
                                novaConexao.execute();
                // informa à interface gráfica que temos
                // uma nova conexão
                mainFrame.insereConexao(novaConexao);
            }
        } catch (IOException ex) {
            return false;
        }
    }

    public void encerraConexao() {
        try {
            if (socket.isClosed() == false) {
                socket.close();
            }
        } catch (IOException ex) {
        }
    }
}
