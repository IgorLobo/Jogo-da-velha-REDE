package br.senai.model;

public final class GameConfig {

    private static GameConfig instancia;
    private static Jogador jogador1;
    private static Jogador jogador2;
    private int rodadas = 0;

    private GameConfig() {
        jogador1 = new Jogador();
        jogador2 = new Jogador();
        rodadas = 0;
    }

    public static synchronized GameConfig getInstance() {
        if (instancia == null) {
            instancia = new GameConfig();
        }
        return instancia;
    }

    public static synchronized void clear() {
        instancia = new GameConfig();
    }
    
    public int getRodadas() {
        return rodadas;
    }

    public void setRodadas(int rodadas) {
        this.rodadas = rodadas;
    }

    public Jogador getJogador1() {
        return jogador1;
    }

    public void setJogador1(Jogador jogador1) {
        GameConfig.jogador1 = jogador1;
    }

    public Jogador getJogador2() {
        return jogador2;
    }

    public void setJogador2(Jogador jogador2) {
        GameConfig.jogador2 = jogador2;
    }

    public Jogador getJogador(Simbolo simbolo) {
        if (jogador1.getSimbolo() == simbolo) {
            return jogador1;
        } else if (jogador2.getSimbolo() == simbolo) {
            return jogador2;
        }
        return null;
    }
}
