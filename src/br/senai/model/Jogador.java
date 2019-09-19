package br.senai.model;

public class Jogador {

    private String apelido = "";
    private int pontos = 0;
    private int vitorias = 0;

    public Jogador() {
    }

    public Jogador(String apelido) {
        this.apelido = apelido;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public int getPontos() {
        return pontos;
    }

    public int getVitorias() {
        return vitorias;
    }

    public void adicionarPonto(int ponto) {
        this.pontos += pontos;
    }

    public void adicionarVitoria(int vitoria) {
        this.vitorias += vitoria;
    }

    public void resetarPontos() {
        pontos = 0;
    }

    public void resetarVitorias() {
        vitorias = 0;
    }

}
