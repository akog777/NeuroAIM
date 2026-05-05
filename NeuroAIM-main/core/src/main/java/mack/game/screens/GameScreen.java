package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameScreen implements Screen {

    // ── Referências ──────────────────────────────────────────────────────────
    private final Game game;
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;

    // ── Estado do jogo ────────────────────────────────────────────────────────
    private enum Estado { JOGANDO, GAME_OVER }
    private Estado estado = Estado.JOGANDO;

    // ── Dimensões da área de jogo ─────────────────────────────────────────────
    private static final float LARGURA  = 800f;
    private static final float ALTURA   = 480f;
    private static final float MARGEM_HUD = 60f; // HUD no topo

    // ── Mira ──────────────────────────────────────────────────────────────────
    private float miraX;
    private float miraY;
    private static final float VELOCIDADE_MIRA = 220f; // px/s
    private static final float RAIO_MIRA       = 14f;

    // Mouse – posição anterior para calcular delta de movimento
    private int mouseXAnterior = -1;
    private int mouseYAnterior = -1;

    // Animação da mira
    private float miraAngulo    = 0f;  // rotação do anel externo
    private float miraPulso     = 0f;  // acumula tempo para efeito de pulso
    private float miraEscala    = 1f;  // pulsa ao acertar/errar

    // ── Estabilidade ─────────────────────────────────────────────────────────
    // Acumula o delta de movimento por frame para medir tremor
    private float movimentoAcumulado  = 0f;
    private float tempoEstabilidade   = 0f;
    private float estabilidadeMedia   = 1f; // 0..1  (1 = muito estável)
    private static final float JANELA_ESTAB = 0.8f; // segundos

    // ── Alvos ─────────────────────────────────────────────────────────────────
    private static final int MAX_ALVOS = 3;
    private final List<Alvo> alvos = new ArrayList<>();
    private float timerProximoAlvo = 0f;
    private float intervaloAlvo    = 2.0f; // segundos entre spawns

    // ── Pontuação ─────────────────────────────────────────────────────────────
    private int    acertos   = 0;
    private int    erros     = 0;
    private float  scoreTotal = 0f;

    // Score final (calculado no game over)
    private float scorePrecisao     = 0f;
    private float scoreEstabilidade = 0f;
    private float scoreReacao       = 0f;
    private float scoreFinal        = 0f;

    // ── Tempo de sessão ───────────────────────────────────────────────────────
    private static final float DURACAO_SESSAO = 60f; // segundos
    private float tempoRestante = DURACAO_SESSAO;

    // ── Feedback visual de acerto ─────────────────────────────────────────────
    private float flashTimer = 0f;
    private boolean acertou  = false; // true = verde, false = vermelho

    // ── Dificuldade (recebe do ConfigScreen futuramente) ──────────────────────
    // Por ora usa Inicial; pode ser injetado via construtor
    private int dificuldade = 1;

    // =========================================================================
    // INNER CLASS – Alvo Neural
    // =========================================================================
    private static class Alvo {
        float x, y;
        float raio;
        float vida;        // segundos até desaparecer
        float vidaTotal;
        float velX, velY;  // movimento lateral (níveis 2+)
        boolean ativo = true;
        float tempoAparecer; // momento em que surgiu (para calcular tempo de reação)

        Alvo(float x, float y, float raio, float vida, float velX, float velY, float agora) {
            this.x = x;
            this.y = y;
            this.raio = raio;
            this.vida = vida;
            this.vidaTotal = vida;
            this.velX = velX;
            this.velY = velY;
            this.tempoAparecer = agora;
        }
    }

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public GameScreen(Game game) {
        this(game, 1);
    }

    public GameScreen(Game game, int dificuldade) {
        this.game = game;
        this.dificuldade = dificuldade;
    }

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================
    @Override
    public void show() {
        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font  = new BitmapFont();

        // Posição inicial da mira: centro da área de jogo
        miraX = LARGURA  / 2f;
        miraY = MARGEM_HUD + (ALTURA - MARGEM_HUD) / 2f;

        // Oculta o cursor do sistema para usar a mira customizada
        Gdx.input.setCursorCatched(true);

        ajustarDificuldade();
    }

    private void ajustarDificuldade() {
        switch (dificuldade) {
            case 1: // Inicial – alvos grandes, estáticos, mais tempo
                intervaloAlvo = 2.2f;
                break;
            case 2: // Intermediário – alvos menores, movimento lateral
                intervaloAlvo = 1.8f;
                break;
            case 3: // Avançado – alvos pequenos, múltiplos, rápidos
                intervaloAlvo = 1.2f;
                break;
        }
    }

    // =========================================================================
    // RENDER PRINCIPAL
    // =========================================================================
    @Override
    public void render(float delta) {
        limparTela();

        if (estado == Estado.JOGANDO) {
            atualizar(delta);
            desenharJogo();
        } else {
            Gdx.input.setCursorCatched(false);
            desenharGameOver();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                game.setScreen(new GameScreen(game, dificuldade));
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    private void limparTela() {
        float r, g, b;
        if (flashTimer > 0) {
            if (acertou) { r=0.05f; g=0.18f; b=0.10f; }  // flash verde escuro
            else         { r=0.20f; g=0.04f; b=0.04f; }  // flash vermelho escuro
        } else {
            // Fundo escuro com leve tonalidade azul-índigo
            r=0.07f; g=0.05f; b=0.13f;
        }
        Gdx.gl.glClearColor(r, g, b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    // =========================================================================
    // LÓGICA
    // =========================================================================
    private void atualizar(float delta) {
        // ── Timer de sessão ───────────────────────────────────────────────────
        tempoRestante -= delta;
        if (tempoRestante <= 0) {
            tempoRestante = 0;
            calcularScoreFinal();
            estado = Estado.GAME_OVER;
            return;
        }

        // ── Cooldown do flash visual ──────────────────────────────────────────
        if (flashTimer > 0) flashTimer -= delta;

        // ── Animação da mira ──────────────────────────────────────────────────
        miraAngulo = (miraAngulo + 30f * delta) % 360f;
        miraPulso  += delta;
        // Escala volta a 1 suavemente após disparo
        miraEscala = MathUtils.lerp(miraEscala, 1f, delta * 10f);

        // ── Mira ──────────────────────────────────────────────────────────────
        moverMira(delta);

        // ── Estabilidade ──────────────────────────────────────────────────────
        tempoEstabilidade += delta;
        if (tempoEstabilidade >= JANELA_ESTAB) {
            // Normaliza: 0 mov = 1.0 estável; muito mov = 0.0 instável
            float maxMov = VELOCIDADE_MIRA * JANELA_ESTAB;
            estabilidadeMedia = 1f - MathUtils.clamp(movimentoAcumulado / maxMov, 0f, 1f);
            movimentoAcumulado = 0f;
            tempoEstabilidade = 0f;
        }

        // ── Spawn de alvos ────────────────────────────────────────────────────
        timerProximoAlvo -= delta;
        if (timerProximoAlvo <= 0 && alvos.size() < MAX_ALVOS) {
            spawnAlvo();
            timerProximoAlvo = intervaloAlvo;
        }

        // ── Atualizar alvos (movimento + expiração) ───────────────────────────
        Iterator<Alvo> it = alvos.iterator();
        while (it.hasNext()) {
            Alvo a = it.next();
            a.vida -= delta;

            // Movimento lateral (nível 2+)
            a.x += a.velX * delta;
            a.y += a.velY * delta;

            // Rebote nas bordas
            float limEsq = a.raio;
            float limDir = LARGURA - a.raio;
            float limBaixo = MARGEM_HUD + a.raio;
            float limCima  = ALTURA - a.raio;
            if (a.x < limEsq || a.x > limDir)  a.velX *= -1;
            if (a.y < limBaixo || a.y > limCima) a.velY *= -1;
            a.x = MathUtils.clamp(a.x, limEsq, limDir);
            a.y = MathUtils.clamp(a.y, limBaixo, limCima);

            // Expirou sem ser atingido
            if (a.vida <= 0) {
                erros++;
                flashTimer = 0.25f;
                acertou = false;
                it.remove();
            }
        }

        // ── Disparo ───────────────────────────────────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            disparar();
        }
    }

    private void moverMira(float delta) {
        // ── Mouse ─────────────────────────────────────────────────────────────
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();

        // Converte coordenada Y do LibGDX (Y=0 no topo) para Y do mundo (Y=0 na base)
        float mundoY = ALTURA - my;

        if (mouseXAnterior >= 0) {
            float deltaMouseX = mx - mouseXAnterior;
            float deltaMouseY = my - mouseYAnterior; // ainda em pixels de tela

            if (Math.abs(deltaMouseX) > 0.01f || Math.abs(deltaMouseY) > 0.01f) {
                miraX += deltaMouseX;
                miraY -= deltaMouseY; // inverte Y
                movimentoAcumulado += Math.sqrt(deltaMouseX * deltaMouseX + deltaMouseY * deltaMouseY);
            }
        }
        mouseXAnterior = mx;
        mouseYAnterior = my;

        // ── Teclado ───────────────────────────────────────────────────────────
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)    || Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)  || Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;

        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            dx /= dist; dy /= dist;
            miraX += dx * VELOCIDADE_MIRA * delta;
            miraY += dy * VELOCIDADE_MIRA * delta;
            movimentoAcumulado += VELOCIDADE_MIRA * delta;
        }

        miraX = MathUtils.clamp(miraX, RAIO_MIRA, LARGURA - RAIO_MIRA);
        miraY = MathUtils.clamp(miraY, MARGEM_HUD + RAIO_MIRA, ALTURA - RAIO_MIRA);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
            game.setScreen(new MenuScreen(game));
        }
    }

    private void spawnAlvo() {
        float raio = getRaioAlvo();
        float margem = raio + 10f;
        float x = MathUtils.random(margem, LARGURA - margem);
        float y = MathUtils.random(MARGEM_HUD + margem, ALTURA - margem);
        float vida = getVidaAlvo();

        float velX = 0, velY = 0;
        if (dificuldade >= 2) {
            velX = MathUtils.random(40f, 90f) * (MathUtils.randomBoolean() ? 1 : -1);
        }
        if (dificuldade == 3) {
            velY = MathUtils.random(30f, 70f) * (MathUtils.randomBoolean() ? 1 : -1);
        }

        float agora = DURACAO_SESSAO - tempoRestante;
        alvos.add(new Alvo(x, y, raio, vida, velX, velY, agora));
    }

    private float getRaioAlvo() {
        switch (dificuldade) {
            case 1: return MathUtils.random(28f, 38f);
            case 2: return MathUtils.random(18f, 28f);
            default: return MathUtils.random(10f, 20f);
        }
    }

    private float getVidaAlvo() {
        switch (dificuldade) {
            case 1: return MathUtils.random(3.5f, 5f);
            case 2: return MathUtils.random(2.5f, 3.5f);
            default: return MathUtils.random(1.5f, 2.5f);
        }
    }

    private void disparar() {
        float melhorDistancia = Float.MAX_VALUE;
        Alvo melhorAlvo = null;

        for (Alvo a : alvos) {
            float dist = new Vector2(miraX - a.x, miraY - a.y).len();
            if (dist < melhorDistancia) {
                melhorDistancia = dist;
                melhorAlvo = a;
            }
        }

        if (melhorAlvo != null && melhorDistancia <= melhorAlvo.raio + RAIO_MIRA) {
            // Acerto!
            float tempoReacao = (DURACAO_SESSAO - tempoRestante) - melhorAlvo.tempoAparecer;
            float tempoReacaoNorm = MathUtils.clamp(1f - (tempoReacao / melhorAlvo.vidaTotal), 0f, 1f);

            // Precisão: quão perto do centro (0 = borda, 1 = centro exato)
            float precisaoNorm = MathUtils.clamp(1f - (melhorDistancia / melhorAlvo.raio), 0f, 1f);

            // Score deste acerto: P*0.5 + E*0.3 + T*0.2  (escala 0..100)
            float scoreParcial = (precisaoNorm * 0.5f + estabilidadeMedia * 0.3f + tempoReacaoNorm * 0.2f) * 100f;
            scoreTotal += scoreParcial;
            acertos++;

            flashTimer = 0.25f;
            acertou = true;
            miraEscala = 1.35f; // pulso de expansão no acerto
            alvos.remove(melhorAlvo);
        } else {
            // Errou (disparou no vazio)
            erros++;
            flashTimer = 0.20f;
            acertou = false;
            miraEscala = 0.70f; // pulso de contração no erro
        }
    }

    private void calcularScoreFinal() {
        int totalTentativas = acertos + erros;
        if (totalTentativas == 0) {
            scoreFinal = 0;
            return;
        }
        // Média do score acumulado por acertos
        scoreFinal = (acertos > 0) ? (scoreTotal / acertos) : 0f;
        // Penalidade proporcional a erros
        float taxaAcerto = (float) acertos / totalTentativas;
        scoreFinal *= taxaAcerto;

        scorePrecisao     = scoreFinal;
        scoreEstabilidade = estabilidadeMedia * 100f;
        scoreReacao       = taxaAcerto * 100f;
    }

    // =========================================================================
    // DESENHO – JOGO
    // =========================================================================
    private void desenharJogo() {
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // ── Grade de fundo sutil ──────────────────────────────────────────────
        // (linhas de grade são desenhadas depois, em Line mode)

        // ── HUD: painel sólido escuro ─────────────────────────────────────────
        shape.setColor(new Color(0.10f, 0.05f, 0.22f, 1f));
        shape.rect(0, ALTURA - MARGEM_HUD, LARGURA, MARGEM_HUD);

        // Faixa de acento na borda inferior do HUD
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 1f));
        shape.rect(0, ALTURA - MARGEM_HUD, LARGURA, 2f);

        // ── Capsulas dos cards HUD ────────────────────────────────────────────
        // Card TEMPO (esquerda)
        desenharCard(10, ALTURA - MARGEM_HUD + 8f, 130f, 44f,
                new Color(0.18f, 0.09f, 0.38f, 1f),
                new Color(0.55f, 0.20f, 0.90f, 1f));

        // Card SCORE (centro)
        desenharCard(LARGURA / 2f - 75f, ALTURA - MARGEM_HUD + 8f, 150f, 44f,
                new Color(0.18f, 0.09f, 0.38f, 1f),
                new Color(0.55f, 0.20f, 0.90f, 1f));

        // Card ACERTOS (direita, metade)
        desenharCard(LARGURA - 265f, ALTURA - MARGEM_HUD + 8f, 120f, 44f,
                new Color(0.05f, 0.20f, 0.12f, 1f),
                new Color(0.15f, 0.75f, 0.40f, 1f));

        // Card ERROS
        desenharCard(LARGURA - 135f, ALTURA - MARGEM_HUD + 8f, 80f, 44f,
                new Color(0.22f, 0.05f, 0.05f, 1f),
                new Color(0.85f, 0.20f, 0.20f, 1f));

        // ── Barra de estabilidade: fundo ──────────────────────────────────────
        // (será desenhada sobre os cards após o text, em pass separado)

        // ── Alvos neurais ─────────────────────────────────────────────────────
        for (Alvo a : alvos) {
            float propVida = a.vida / a.vidaTotal;
            float urgencia = 1f - propVida; // 0=recém nascido, 1=expirando

            // Aura pulsante externa
            float auraRaio = a.raio + 12f + MathUtils.sin(miraPulso * 4f + a.x) * 3f;
            shape.setColor(new Color(
                    0.40f + 0.35f * urgencia,
                    0.05f,
                    0.75f - 0.45f * urgencia,
                    0.12f + 0.08f * propVida));
            shape.circle(a.x, a.y, auraRaio);

            // Anel externo (fino)
            shape.setColor(new Color(
                    0.60f + 0.30f * urgencia,
                    0.10f,
                    0.85f - 0.50f * urgencia,
                    0.30f));
            shape.circle(a.x, a.y, a.raio + 5f);

            // Corpo principal do alvo – cor muda de roxo -> vermelho
            shape.setColor(new Color(
                    0.28f + 0.42f * urgencia,
                    0.06f,
                    0.72f - 0.42f * urgencia,
                    0.92f));
            shape.circle(a.x, a.y, a.raio);

            shape.setColor(new Color(1f, 1f, 1f, 0.12f));
            shape.circle(a.x, a.y, a.raio * 0.72f);

            shape.setColor(new Color(1f, 1f, 1f, 0.85f));
            shape.circle(a.x, a.y, a.raio * 0.18f);

            float bvLarg = a.raio * 2f;
            float bvX = a.x - a.raio;
            float bvY = a.y - a.raio - 8f;
            shape.setColor(new Color(0.15f, 0.05f, 0.25f, 0.7f));
            shape.rect(bvX, bvY, bvLarg, 4f);
            shape.setColor(new Color(0.15f + 0.7f * urgencia, 0.75f - 0.65f * urgencia, 0.20f, 0.9f));
            shape.rect(bvX, bvY, bvLarg * propVida, 4f);
        }

        boolean sobreAlvo = false;
        for (Alvo a : alvos) {
            if (new Vector2(miraX - a.x, miraY - a.y).len() <= a.raio + RAIO_MIRA) {
                sobreAlvo = true;
                break;
            }
        }
        Color corMira    = sobreAlvo ? new Color(0.15f, 0.95f, 0.45f, 1f) : new Color(0.85f, 0.85f, 1.00f, 1f);
        Color corMiraHalo= sobreAlvo ? new Color(0.15f, 0.95f, 0.45f, 0.12f) : new Color(0.55f, 0.20f, 0.90f, 0.10f);

        float pulsoRaio = RAIO_MIRA + 11f + MathUtils.sin(miraPulso * 3.2f) * 2f;
        float escR = miraEscala;

        shape.setColor(corMiraHalo);
        shape.circle(miraX, miraY, (pulsoRaio + 14f) * escR);

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.06f));
        float passo = 40f;
        for (float gx = 0; gx < LARGURA; gx += passo)
            shape.line(gx, MARGEM_HUD, gx, ALTURA - MARGEM_HUD);
        for (float gy = MARGEM_HUD; gy < ALTURA - MARGEM_HUD; gy += passo)
            shape.line(0, gy, LARGURA, gy);

        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.4f));
        shape.rect(10, ALTURA - MARGEM_HUD + 8f, 130f, 44f);
        shape.rect(LARGURA / 2f - 75f, ALTURA - MARGEM_HUD + 8f, 150f, 44f);
        shape.setColor(new Color(0.15f, 0.75f, 0.40f, 0.5f));
        shape.rect(LARGURA - 265f, ALTURA - MARGEM_HUD + 8f, 120f, 44f);
        shape.setColor(new Color(0.85f, 0.20f, 0.20f, 0.5f));
        shape.rect(LARGURA - 135f, ALTURA - MARGEM_HUD + 8f, 80f, 44f);

        float bEstX = 10f, bEstY = ALTURA - MARGEM_HUD + 5f;
        float bEstLarg = LARGURA - 20f, bEstAltura = 3f;
        shape.setColor(new Color(0.30f, 0.10f, 0.50f, 0.4f));
        shape.line(bEstX, bEstY, bEstX + bEstLarg, bEstY);

        shape.setColor(corMira);
        shape.circle(miraX, miraY, pulsoRaio * escR);
        shape.circle(miraX, miraY, RAIO_MIRA * escR);

        float rMedio = (RAIO_MIRA + pulsoRaio) * 0.5f * escR;
        for (int s = 0; s < 4; s++) {
            float ini = miraAngulo + s * 90f;
            desenharArco(ini, ini + 50f, rMedio, 16);
        }

        float braco = (pulsoRaio + 16f) * escR;
        float gap   = (RAIO_MIRA + 5f)  * escR;
        shape.line(miraX - braco, miraY, miraX - gap, miraY);
        shape.line(miraX + gap,   miraY, miraX + braco, miraY);
        shape.line(miraX, miraY - braco, miraX, miraY - gap);
        shape.line(miraX, miraY + gap,   miraX, miraY + braco);

        float diagR = RAIO_MIRA * 0.55f * escR;
        float diagLen = 5f * escR;
        for (int q = 0; q < 4; q++) {
            float ang = MathUtils.degreesToRadians * (45f + q * 90f);
            float cx  = miraX + MathUtils.cos(ang) * diagR;
            float cy  = miraY + MathUtils.sin(ang) * diagR;
            shape.line(cx, cy, miraX + MathUtils.cos(ang) * (diagR + diagLen), miraY + MathUtils.sin(ang) * (diagR + diagLen));
        }

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(corMira);
        shape.circle(miraX, miraY, 3.5f * escR);

        float estabFill = LARGURA * estabilidadeMedia;
        Color corEstab = estabilidadeMedia > 0.6f ? new Color(0.20f, 0.85f, 0.45f, 0.7f) : new Color(0.90f, 0.25f, 0.25f, 0.7f);
        shape.setColor(corEstab);
        shape.rect(0, ALTURA - MARGEM_HUD, estabFill, 3f);

        shape.end();

        batch.begin();

        int seg = (int) tempoRestante;
        boolean urgenciaTimer = tempoRestante < 10f;
        font.getData().setScale(1.3f);
        font.setColor(urgenciaTimer ? new Color(1f, 0.35f, 0.35f, 1f) : new Color(0.85f, 0.70f, 1.00f, 1f));
        font.draw(batch, "TEMPO", 20, ALTURA - MARGEM_HUD + 48f);
        font.getData().setScale(1.8f);
        font.setColor(urgenciaTimer ? new Color(1f, 0.25f, 0.25f, 1f) : Color.WHITE);
        font.draw(batch, String.format("%02d:%02d", seg / 60, seg % 60), 20, ALTURA - MARGEM_HUD + 32f);

        font.getData().setScale(1.3f);
        font.setColor(new Color(0.85f, 0.70f, 1.00f, 1f));
        font.draw(batch, "SCORE", LARGURA / 2f - 65f, ALTURA - MARGEM_HUD + 48f);
        font.getData().setScale(1.8f);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf((int) scoreTotal), LARGURA / 2f - 65f, ALTURA - MARGEM_HUD + 32f);

        font.getData().setScale(1.1f);
        font.setColor(new Color(0.50f, 1.00f, 0.65f, 1f));
        font.draw(batch, "ACERTOS", LARGURA - 260f, ALTURA - MARGEM_HUD + 48f);
        font.getData().setScale(1.8f);
        font.setColor(new Color(0.30f, 1.00f, 0.55f, 1f));
        font.draw(batch, String.valueOf(acertos), LARGURA - 210f, ALTURA - MARGEM_HUD + 32f);

        font.getData().setScale(1.1f);
        font.setColor(new Color(1.00f, 0.50f, 0.50f, 1f));
        font.draw(batch, "ERROS", LARGURA - 130f, ALTURA - MARGEM_HUD + 48f);
        font.getData().setScale(1.8f);
        font.setColor(new Color(1f, 0.30f, 0.30f, 1f));
        font.draw(batch, String.valueOf(erros), LARGURA - 107f, ALTURA - MARGEM_HUD + 32f);

        font.getData().setScale(0.9f);
        font.setColor(new Color(0.60f, 0.60f, 0.75f, 1f));
        String labelEstab = estabilidadeMedia > 0.6f ? "ESTAVEL" : "INSTAVEL";
        font.draw(batch, "ESTAB: " + labelEstab + "  (" + (int)(estabilidadeMedia * 100) + "%)",
                10, ALTURA - MARGEM_HUD - 4f);

        font.getData().setScale(0.85f);
        font.setColor(new Color(0.45f, 0.45f, 0.60f, 1f));
        font.draw(batch, "Mouse / W A S D  -  ESPACO ou CLIQUE para disparar  -  ESC: menu", 10, 14f);

        batch.end();
    }

    private void desenharGameOver() {
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(new Color(0.08f, 0.04f, 0.18f, 1f));
        shape.rect(80, 60, LARGURA - 160f, ALTURA - 120f);

        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 1f));
        shape.rect(80, 60, 4f, ALTURA - 120f);

        shape.setColor(new Color(0.14f, 0.07f, 0.28f, 1f));
        shape.rect(84, ALTURA - 130f, LARGURA - 164f, 50f);

        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.25f));
        shape.rect(100, 175f, LARGURA - 200f, 2f);

        shape.setColor(new Color(0.20f, 0.09f, 0.40f, 0.6f));
        shape.rect(100, 120f, LARGURA - 200f, 50f);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.4f));
        shape.rect(100, 120f, 4f, 50f);

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.5f));
        shape.rect(80, 60, LARGURA - 160f, ALTURA - 120f);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.35f));
        shape.rect(100, 120f, LARGURA - 200f, 50f);
        shape.end();

        batch.begin();

        font.getData().setScale(2.6f);
        font.setColor(Color.WHITE);
        font.draw(batch, "SESSAO CONCLUIDA", 105, ALTURA - 90f);

        font.getData().setScale(1.1f);
        font.setColor(new Color(0.65f, 0.55f, 0.85f, 1f));
        font.draw(batch, "Score = (P x 0.5)  +  (E x 0.3)  +  (T x 0.2)", 110, ALTURA - 145f);

        float linhaY = ALTURA - 180f;
        float colLabel = 110f, colVal = 530f;

        font.getData().setScale(1.3f);
        font.setColor(new Color(0.55f, 0.75f, 1.00f, 1f));
        font.draw(batch, "Precisao  (P)", colLabel, linhaY);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f%%", scorePrecisao), colVal, linhaY);

        linhaY -= 35f;
        font.setColor(new Color(0.30f, 1.00f, 0.60f, 1f));
        font.draw(batch, "Estabilidade  (E)", colLabel, linhaY);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f%%", scoreEstabilidade), colVal, linhaY);

        linhaY -= 35f;
        font.setColor(new Color(1.00f, 0.75f, 0.30f, 1f));
        font.draw(batch, "Reacao  (T)", colLabel, linhaY);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%d / %d acertos", acertos, acertos + erros), colVal - 40f, linhaY);

        font.getData().setScale(2.2f);
        font.setColor(new Color(0.75f, 0.45f, 1.00f, 1f));
        font.draw(batch, "SCORE FINAL", 115, 160f);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f", scoreFinal), 460f, 160f);

        font.getData().setScale(1.1f);
        font.setColor(new Color(0.55f, 0.55f, 0.70f, 1f));
        font.draw(batch, "ENTER: jogar novamente     ESC: menu principal", 130f, 88f);

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
    }

    private void desenharCard(float x, float y, float largura, float altura, Color corFundo, Color corAcento) {
        shape.setColor(corFundo);
        shape.rect(x, y, largura, altura);
        shape.setColor(corAcento);
        shape.rect(x, y, 3f, altura);
    }

    private void desenharArco(float inicio, float fim, float raio, int passos) {
        float passo = (fim - inicio) / passos;
        for (int i = 0; i < passos; i++) {
            float a1 = MathUtils.degreesToRadians * (inicio + i * passo);
            float a2 = MathUtils.degreesToRadians * (inicio + (i + 1) * passo);
            shape.line(
                miraX + MathUtils.cos(a1) * raio, miraY + MathUtils.sin(a1) * raio,
                miraX + MathUtils.cos(a2) * raio, miraY + MathUtils.sin(a2) * raio
            );
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
    }
}