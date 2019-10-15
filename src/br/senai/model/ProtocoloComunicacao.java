package br.senai.model;

/**
 *
 * @author Hygor
 */
public class ProtocoloComunicacao {

    private String id;
    private int tamanho;
    private String mensagem;
    private String posicao;

    public ProtocoloComunicacao() {
    }

    public String enviarMensagem(String mensagem) {
        StringBuilder st = new StringBuilder(mensagem);
        st.insert(2, String.format("%03d", st.length() + 3));
        return st.toString();
    }

    public void decifrarMensagem(String mensagem) {
        char[] decifrado = mensagem.toCharArray();
        id = String.copyValueOf(decifrado, 0, 2);
        tamanho = Integer.parseInt(String.copyValueOf(decifrado, 2, 3));
        this.mensagem = String.copyValueOf(decifrado, 5, mensagem.length() - 5);
    }

    public String getId() {
        return id;
    }

    public int getTamanho() {
        return tamanho;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getPosicao() {
        return posicao;
    }

    public void setPosicao(String posicao) {
        this.posicao = posicao;
    } 
    
    public String verificarMensagemRecebida(String msg) {
        decifrarMensagem(msg);
        return id;
    }

    public String verificarMensagemRecebidaTCP(String msg) {
        decifrarMensagemTCP(msg);
        return id;
    }

    private void decifrarMensagemTCP(String msg) {
        char[] decifrado = msg.toCharArray();
        id = String.copyValueOf(decifrado, 0, 2);
        tamanho = Integer.parseInt(String.copyValueOf(decifrado, 2, 3));
        this.posicao = String.copyValueOf(decifrado, 5, msg.length() - 5);
    }
}
