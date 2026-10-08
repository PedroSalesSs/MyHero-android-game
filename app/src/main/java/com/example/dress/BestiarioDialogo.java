package com.example.dress;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.example.dress.data.Catalogo;
import com.example.dress.databinding.DialogoBestiarioBinding;
import com.example.dress.databinding.ItemBestiarioBinding;
import com.example.dress.model.Batalha;
import com.example.dress.model.Inimigo;
import com.example.dress.util.SpriteLoader;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Bestiário: a janela com as particularidades de cada inimigo, aberta pelo
 * botão "?" na escolha do inimigo.
 *
 * Nenhum número desta janela é escrito à mão: tudo vem das constantes da Batalha
 * e da ficha de cada Inimigo. Se o balanceamento mudar, a janela acompanha.
 * E, como os cartões são criados num laço sobre Inimigo.values(), um inimigo novo
 * aparece aqui sozinho (só precisa de uma dica no Catalogo).
 */
public final class BestiarioDialogo {

    /** Multiplicadores no formato brasileiro: 2,5 / 1,25 / 3 (sem ",0"). */
    private static final DecimalFormat FORMATO_MULTIPLICADOR =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));

    private BestiarioDialogo() {
        // Só tem o método estático mostrar(); não é para criar objetos desta classe.
    }

    /** Abre a janela do Bestiário por cima da tela atual. */
    public static void mostrar(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        DialogoBestiarioBinding dialogo = DialogoBestiarioBinding.inflate(inflater);
        AlertDialog janela = new AlertDialog.Builder(context)
                .setView(dialogo.getRoot())
                .create();

        dialogo.txtRegras.setText(montarRegras(context));

        for (Inimigo inimigo : Inimigo.values()) {
            // O "true" já coloca o cartão dentro da lista
            ItemBestiarioBinding cartao = ItemBestiarioBinding.inflate(inflater, dialogo.listaBestiario, true);
            preencherCartao(context, cartao, inimigo);
        }

        dialogo.btnVoltar.setOnClickListener(v -> janela.dismiss());

        // Fundo transparente: quem desenha a janela é o nosso pergaminho, não o tema padrão
        if (janela.getWindow() != null) {
            janela.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        janela.show();
    }

    // ===================================================================== regras gerais

    /** Monta a lista de regras do combate, uma por linha, com um "•" na frente. */
    private static CharSequence montarRegras(Context context) {
        String[] regras = {
                context.getString(R.string.regra_ataque,
                        Batalha.DANO_HEROI_MINIMO, Batalha.DANO_HEROI_MAXIMO, Batalha.VIDA_HEROI),
                context.getString(R.string.regra_preparo,
                        porcentagem(Batalha.CHANCE_PREPARAR_GOLPE)),
                context.getString(R.string.regra_defesa,
                        porcentagem(1 - Batalha.FRACAO_DANO_DEFENDENDO),
                        porcentagem(Batalha.CHANCE_BLOQUEIO_PERFEITO),
                        Batalha.DANO_HEROI_MINIMO, Batalha.DANO_HEROI_MAXIMO),
                context.getString(R.string.regra_defesa_turno),
                context.getString(R.string.regra_especiais,
                        Batalha.USOS_ATAQUE_PESADO, porcentagem(Batalha.CHANCE_PESADO_CRITICO),
                        Batalha.USOS_POCAO, Batalha.CURA_POCAO)
        };

        SpannableStringBuilder texto = new SpannableStringBuilder();
        for (int i = 0; i < regras.length; i++) {
            if (i > 0) texto.append("\n");
            texto.append("• ").append(html(regras[i]));
        }
        return texto;
    }

    // ===================================================================== cartão de cada inimigo

    private static void preencherCartao(Context context, ItemBestiarioBinding cartao, Inimigo inimigo) {
        // Cabeçalho: retrato (sem suavização, pixel art nítida), nome, estrelas e ficha
        cartao.imgInimigo.setImageBitmap(
                SpriteLoader.carregarRetratoInimigo(context, Catalogo.getImagemInimigo(inimigo)));
        cartao.imgInimigo.getDrawable().setFilterBitmap(false);
        cartao.txtNome.setText(inimigo.getNome());
        cartao.txtEstrelas.setText(estrelas(context, inimigo.getDificuldade(), Inimigo.values().length));
        cartao.txtFicha.setText(context.getString(R.string.bestiario_ficha,
                inimigo.getVidaMaxima(), inimigo.getDanoMinimo(), inimigo.getDanoMaximo()));

        // Golpe especial: nome e como funciona
        cartao.txtGolpe.setText(context.getString(R.string.bestiario_golpe, inimigo.getNomeGolpePoderoso()));
        cartao.txtGolpeRegra.setText(context.getString(R.string.bestiario_golpe_regra,
                inimigo.getAvisoGolpePoderoso(), descreverGolpe(context, inimigo)));

        // Tabela de dano do golpe especial
        int golpes = inimigo.getGolpesPoderosos();
        int minimo = inimigo.getDanoPoderosoMinimo();
        int maximo = inimigo.getDanoPoderosoMaximo();
        cartao.txtSemDefender.setText(faixa(context, golpes, minimo, maximo));
        cartao.txtDefendendo.setText(faixa(context, golpes,
                Batalha.reduzirPelaDefesa(minimo), Batalha.reduzirPelaDefesa(maximo)));

        if (!inimigo.isPoderosoBloqueavel()) {
            cartao.txtBloqueio.setText(R.string.bloqueio_impossivel);
            cartao.txtBloqueio.setTextColor(ContextCompat.getColor(context, R.color.vermelho_tinta));
        } else if (golpes > 1) {
            cartao.txtBloqueio.setText(R.string.bloqueio_cada_golpe);
        } else {
            cartao.txtBloqueio.setText(context.getString(R.string.bloqueio_funciona,
                    porcentagem(Batalha.CHANCE_BLOQUEIO_PERFEITO)));
        }

        // Dica de estratégia (o único texto escrito à mão para cada inimigo)
        cartao.txtDica.setText(html(context.getString(R.string.dica,
                context.getString(Catalogo.getDicaInimigo(inimigo)))));
    }

    /** "Um golpe com 3× o dano." ou "2 golpes seguidos de 1,25× cada." (+ aviso se atravessa a guarda) */
    private static String descreverGolpe(Context context, Inimigo inimigo) {
        String multiplicador = FORMATO_MULTIPLICADOR.format(inimigo.getMultiplicadorPoderoso());
        String descricao = inimigo.getGolpesPoderosos() > 1
                ? context.getString(R.string.golpe_varios, inimigo.getGolpesPoderosos(), multiplicador)
                : context.getString(R.string.golpe_um, multiplicador);

        if (!inimigo.isPoderosoBloqueavel()) {
            descricao += " " + context.getString(R.string.golpe_atravessa);
        }
        return descricao;
    }

    /**
     * Faixa de dano. Com um golpe: "45–63". Com vários, mostra o total e,
     * entre parênteses, cada golpe: "36–50 (2×18–25)".
     */
    private static String faixa(Context context, int golpes, int minimo, int maximo) {
        if (golpes > 1) {
            return context.getString(R.string.faixa_dano_varios,
                    minimo * golpes, maximo * golpes, golpes, minimo, maximo);
        }
        return context.getString(R.string.faixa_dano, minimo, maximo);
    }

    /** Estrelas de dificuldade: as acesas em dourado e as que faltam bem apagadas. */
    private static CharSequence estrelas(Context context, int acesas, int total) {
        SpannableStringBuilder texto = new SpannableStringBuilder();
        for (int i = 0; i < total; i++) {
            texto.append("★");
        }
        // Só pinta se sobrou estrela apagada: o Android não aceita um span de tamanho zero
        if (acesas < total) {
            int corApagada = ContextCompat.getColor(context, R.color.dourado_estrela_vazia);
            texto.setSpan(new ForegroundColorSpan(corApagada), acesas, total, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return texto;
    }

    // ===================================================================== auxiliares

    /** 0.35 vira 35 (para mostrar como "35%"). */
    private static int porcentagem(double fracao) {
        return (int) Math.round(fracao * 100);
    }

    /** Aplica os <b>, <i> e <font> dos textos do strings.xml. */
    private static Spanned html(String texto) {
        return HtmlCompat.fromHtml(texto, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }
}
