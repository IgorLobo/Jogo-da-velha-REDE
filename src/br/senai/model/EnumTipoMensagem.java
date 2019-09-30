package br.senai.model;

/**
 *
 * @author Hygor
 */
public enum EnumTipoMensagem {
    UM("01"), 
    DOIS("02"), 
    TRES("03"), 
    QUATRO("04"), 
    CINCO("05"), 
    SEIS("06"), 
    SETE("07"), 
    OITO("08"), 
    NOVE("09");
    
    String descricao;

    private EnumTipoMensagem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
