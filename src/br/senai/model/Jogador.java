package br.senai.model;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Jogador {

    private String apelido = "";
    private int pontos = 0;
    private int vitorias = 0;
    private Simbolo simbolo = null;
    private String ip = "";
    private int port = 0;
    private Socket socket;

    public Jogador() {
    }

    public Jogador(String apelido) {
        this.apelido = apelido;
    }

    public Jogador(String apelido, String ip, int porta) {
        this.apelido = apelido;
        this.ip = ip;
        this.port = porta;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public Simbolo getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(Simbolo simbolo) {
        this.simbolo = simbolo;
    }

    public int getPontos() {
        return pontos;
    }

    public int getVitorias() {
        return vitorias;
    }

    public void adicionarPonto(int ponto) {
        this.pontos += ponto;
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

    public Socket getSocket() throws IOException {
        return new Socket(this.ip, this.port);
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
