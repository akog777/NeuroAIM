package mack.game.screens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {

    private final Game game;
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;
    private Texture background;
    private Music musicaFundo;

    private boolean botaoAnterior = false;

    private OrthographicCamera camera;
    private Viewport viewport;

    private enum Estado { JOGANDO, GAME_OVER, PAUSADO }
    private Estado estado = Estado.JOGANDO;

    // --- VARIÁVEIS DO MENU DE PAUSA ---
    private String[] opcoesPausa = { "CONTINUAR", "REINICIAR FASE", "VOLTAR AO MENU" };
    private int opcaoPausaSelecionada = 0;

    private static final float LARGURA    = 800f;
    private static final float ALTURA     = 480f;
    private static final float MARGEM_HUD = 60f;

    private float miraX;
    private float miraY;
    private mack.game.JoystickInput joystick;
    private Thread joystickThread;
    private static final float VELOCIDADE_MIRA = 220f;
    private static final float RAIO_MIRA       = 8f; 
    private static final float MARGEM_TOPO     = 60f;

    private float mouseXAnterior = -1;
    private float mouseYAnterior = -1;

    private float miraAngulo = 0f;
    private float miraPulso  = 0f;
    private float miraEscala = 1f;

    private float movimentoAcumulado = 0f;
    private float tempoEstabilidade  = 0f;
    private float estabilidadeMedia  = 1f;
    private static final float JANELA_ESTAB = 0.8f;

    private static final int MAX_ALVOS = 3;
    private final List<Alvo> alvos = new ArrayList<>();
    private float timerProximoAlvo = 0f;
    private float intervaloAlvo    = 2.0f;

    private int   acertos    = 0;
    private int   erros      = 0;
    private float scoreTotal = 0f;

    private float scorePrecisao     = 0f;
    private float scoreEstabilidade = 0f;
    private float scoreReacao       = 0f;
    private float scoreFinal        = 0f;

    private static final float DURACAO_SESSAO = 60f;
    private float tempoRestante = DURACAO_SESSAO;

    private float   flashTimer = 0f;
    private boolean acertou    = false;

    private int dificuldade = 1;

    // =========================================================================
    // INNER CLASS
    // =========================================================================
    private static class Alvo {
        float x, y, raio, vida, vidaTotal, velX, velY, tempoAparecer;

        Alvo(float x, float y, float raio, float vida, float velX, float velY, float agora) {
            this.x = x; this.y = y; this.raio = raio;
            this.vida = vida; this.vidaTotal = vida;
            this.velX = velX; this.velY = velY;
            this.tempoAparecer = agora;
        }
    }

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public GameScreen(Game game) { this(game, 1); }

    public GameScreen(Game game, int dificuldade) {
        this.game = game;
        this.dificuldade = dificuldade;
    }

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================
    @Override
    public void show() {
        camera   = new OrthographicCamera();
        viewport = new StretchViewport(LARGURA, ALTURA, camera);
        viewport.apply();
        camera.position.set(LARGURA / 2f, ALTURA / 2f, 0);

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font  = new BitmapFont();

        miraX = LARGURA / 2f;
        miraY = MARGEM_HUD + (ALTURA - MARGEM_HUD) / 2f;

            
        String bgPath = "fase" + dificuldade + ".jpeg";
        background = new Texture(Gdx.files.internal(bgPath));

        // --- CARREGAR MÚSICA DA FASE DINAMICAMENTE ---
        String musicPath = "fase" + dificuldade + "m.mp3";
        musicaFundo = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
        musicaFundo.setLooping(true); // Repete a música se ela acabar antes do tempo
        musicaFundo.setVolume(0.3f);  // Volume entre 0.0f (mudo) e 1.0f (estourando)
        musicaFundo.play();

        Gdx.input.setCursorCatched(true);
        ajustarDificuldade();
        
        joystick = new mack.game.JoystickInput("COM4");
        joystickThread = new Thread(joystick);
        joystickThread.setDaemon(true);
        joystickThread.start();
    }

    private void ajustarDificuldade() {
        switch (dificuldade) {
            case 1: intervaloAlvo = 2.2f; break;
            case 2: intervaloAlvo = 1.8f; break;
            case 3: intervaloAlvo = 1.2f; break;
        }
    }

    // =========================================================================
    // RENDER
    // =========================================================================
    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // --- SISTEMA DE PAUSA ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (estado == Estado.JOGANDO) {
                estado = Estado.PAUSADO;
                if (musicaFundo != null) musicaFundo.pause(); // Pausa a música
            } else if (estado == Estado.PAUSADO) {
                estado = Estado.JOGANDO;
                if (musicaFundo != null) musicaFundo.play();  // Despausa a música
                Gdx.input.setCursorCatched(true); // Prende o mouse de novo
            }
        }

        limparTela();

        batch.begin();
        batch.draw(background, 0, 0, LARGURA, ALTURA);
        batch.end();

        if (estado == Estado.JOGANDO) {
            atualizar(delta);
            desenharJogo();
        } else if (estado == Estado.PAUSADO) {
            Gdx.input.setCursorCatched(false);
            desenharJogo(); // Desenha o jogo congelado no fundo
            atualizarPause();
            desenharPause();
        } else {
            Gdx.input.setCursorCatched(false);
            desenharGameOver();
            
            boolean apertouParaAvancar = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
                                         (joystick != null && joystick.botaoPressionado);

            if (apertouParaAvancar) {
                if (joystick != null) joystick.botaoPressionado = false; // Reseta o clique

                // --- LÓGICA DE META DE ACERTOS ---
                boolean passou = false;
                if (dificuldade == 1 && scoreReacao >= 75f) passou = true;
                else if (dificuldade == 2 && scoreReacao >= 50f) passou = true;
                else if (dificuldade == 3 && scoreReacao >= 30f) passou = true;

                if (passou) {
                    // Bateu a meta! Mostra a história e avança
                    game.setScreen(new StoryScreen(game, dificuldade));
                } else {
                    // Falhou na meta. Repete a mesma fase
                    game.setScreen(new GameScreen(game, dificuldade));
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private void limparTela() {
        float r, g, b;
        if (flashTimer > 0) {
            if (acertou) { r=0.05f; g=0.18f; b=0.10f; }
            else         { r=0.20f; g=0.04f; b=0.04f; }
        } else {
            r=0.07f; g=0.05f; b=0.13f;
        }
        Gdx.gl.glClearColor(r, g, b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    // =========================================================================
    // LÓGICA
    // =========================================================================
    private void atualizar(float delta) {
        tempoRestante -= delta;
        if (tempoRestante <= 0) {
            tempoRestante = 0;
            calcularScoreFinal();
            estado = Estado.GAME_OVER;
            return;
        }

        if (flashTimer > 0) flashTimer -= delta;

        miraAngulo = (miraAngulo + 30f * delta) % 360f;
        miraPulso  += delta;
        miraEscala  = MathUtils.lerp(miraEscala, 1f, delta * 10f);

        moverMira(delta);

        tempoEstabilidade += delta;
        if (tempoEstabilidade >= JANELA_ESTAB) {
            float maxMov = VELOCIDADE_MIRA * JANELA_ESTAB;
            estabilidadeMedia  = 1f - MathUtils.clamp(movimentoAcumulado / maxMov, 0f, 1f);
            movimentoAcumulado = 0f;
            tempoEstabilidade  = 0f;
        }

        timerProximoAlvo -= delta;
        if (timerProximoAlvo <= 0 && alvos.size() < MAX_ALVOS) {
            spawnAlvo();
            timerProximoAlvo = intervaloAlvo;
        }

        Iterator<Alvo> it = alvos.iterator();
        while (it.hasNext()) {
            Alvo a = it.next();
            a.vida -= delta;
            a.x += a.velX * delta;
            a.y += a.velY * delta;

            float limEsq   = a.raio;
            float limDir   = LARGURA - a.raio;
            float limBaixo = MARGEM_HUD + a.raio;
            float limCima  = ALTURA - MARGEM_TOPO - a.raio;
            if (a.x < limEsq   || a.x > limDir)   a.velX *= -1;
            if (a.y < limBaixo || a.y > limCima)   a.velY *= -1;
            a.x = MathUtils.clamp(a.x, limEsq,   limDir);
            a.y = MathUtils.clamp(a.y, limBaixo, limCima);

            if (a.vida <= 0) {
                erros++;
                flashTimer = 0.25f;
                acertou = false;
                it.remove();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            disparar();
        }
    }

    private void moverMira(float delta) {
        float dx = 0, dy = 0;

        if (joystick != null) {
            
            // --- EIXO X DO JOGO (ESQUERDA / DIREITA) ---
            if (joystick.y < 1000) dx -= 1;      
            else if (joystick.y > 3000) dx += 1; 

            // --- EIXO Y DO JOGO (BAIXO / CIMA) ---
            if (joystick.x < 1000) dy -= 1;      
            else if (joystick.x > 3000) dy += 1; 

            // --- LÓGICA DO BOTÃO (1 CLIQUE POR APERTO) ---
            boolean botaoAtual = joystick.botaoPressionado;
            
            // Só dispara se estiver pressionado AGORA e NÃO ESTAVA no frame passado
            if (botaoAtual && !botaoAnterior) {
                disparar();
            }
            
            // Guarda o estado atual para comparar no próximo frame
            botaoAnterior = botaoAtual;
        }

        // Teclado (Mantido para testes)
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
    }

    private void spawnAlvo() {
        float raio   = getRaioAlvo();
        float margem = raio + 10f;
        float x      = MathUtils.random(margem, LARGURA - margem);
        float y      = MathUtils.random(MARGEM_HUD + margem, ALTURA - MARGEM_TOPO - margem);
        float vida   = getVidaAlvo();

        float velX = 0, velY = 0;
        if (dificuldade >= 2)
            velX = MathUtils.random(40f, 90f) * (MathUtils.randomBoolean() ? 1 : -1);
        if (dificuldade == 3)
            velY = MathUtils.random(30f, 70f) * (MathUtils.randomBoolean() ? 1 : -1);

        float agora = DURACAO_SESSAO - tempoRestante;
        alvos.add(new Alvo(x, y, raio, vida, velX, velY, agora));
    }

    private float getRaioAlvo() {
        switch (dificuldade) {
            case 1:  return MathUtils.random(28f, 38f);
            case 2:  return MathUtils.random(18f, 28f);
            default: return MathUtils.random(10f, 20f);
        }
    }

    private float getVidaAlvo() {
        switch (dificuldade) {
            case 1:  return MathUtils.random(3.5f, 5f);
            case 2:  return MathUtils.random(2.5f, 3.5f);
            default: return MathUtils.random(1.5f, 2.5f);
        }
    }

    private void disparar() {
        float melhorDistancia = Float.MAX_VALUE;
        Alvo  melhorAlvo      = null;

        for (Alvo a : alvos) {
            float dist = new Vector2(miraX - a.x, miraY - a.y).len();
            if (dist < melhorDistancia) { melhorDistancia = dist; melhorAlvo = a; }
        }

        if (melhorAlvo != null && melhorDistancia <= melhorAlvo.raio + RAIO_MIRA) {
            float tempoReacao     = (DURACAO_SESSAO - tempoRestante) - melhorAlvo.tempoAparecer;
            float tempoReacaoNorm = MathUtils.clamp(1f - (tempoReacao / melhorAlvo.vidaTotal), 0f, 1f);
            float precisaoNorm    = MathUtils.clamp(1f - (melhorDistancia / melhorAlvo.raio), 0f, 1f);
            float scoreParcial    = (precisaoNorm * 0.5f + estabilidadeMedia * 0.3f + tempoReacaoNorm * 0.2f) * 100f;
            scoreTotal += scoreParcial;
            acertos++;
            flashTimer = 0.25f;
            acertou    = true;
            miraEscala = 1.35f;
            alvos.remove(melhorAlvo);
        } else {
            erros++;
            flashTimer = 0.20f;
            acertou    = false;
            miraEscala = 0.70f;
        }
    }

    private void calcularScoreFinal() {
        int totalTentativas = acertos + erros;
        if (totalTentativas == 0) { scoreFinal = 0; return; }
        scoreFinal = (acertos > 0) ? (scoreTotal / acertos) : 0f;
        float taxaAcerto = (float) acertos / totalTentativas;
        scoreFinal       *= taxaAcerto;
        scorePrecisao     = scoreFinal;
        scoreEstabilidade = estabilidadeMedia * 100f;
        scoreReacao       = taxaAcerto * 100f;
    }

    // =========================================================================
    // DESENHO – JOGO
    // =========================================================================
    private void desenharJogo() {
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(new Color(0.10f, 0.05f, 0.22f, 1f));
        shape.rect(0, ALTURA - MARGEM_HUD, LARGURA, MARGEM_HUD);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 1f));
        shape.rect(0, ALTURA - MARGEM_HUD, LARGURA, 2f);

        desenharCard(10, ALTURA - MARGEM_HUD + 8f, 130f, 44f,
                new Color(0.18f, 0.09f, 0.38f, 1f), new Color(0.55f, 0.20f, 0.90f, 1f));
        desenharCard(LARGURA - 265f, ALTURA - MARGEM_HUD + 8f, 120f, 44f,
                new Color(0.05f, 0.20f, 0.12f, 1f), new Color(0.15f, 0.75f, 0.40f, 1f));
        desenharCard(LARGURA - 135f, ALTURA - MARGEM_HUD + 8f, 80f, 44f,
                new Color(0.22f, 0.05f, 0.05f, 1f), new Color(0.85f, 0.20f, 0.20f, 1f));

        for (Alvo a : alvos) {
            float propVida = a.vida / a.vidaTotal;
            float urgencia = 1f - propVida;

            shape.setColor(new Color(0.40f + 0.35f * urgencia, 0.05f, 0.75f - 0.45f * urgencia, 0.12f + 0.08f * propVida));
            shape.circle(a.x, a.y, a.raio + 12f + MathUtils.sin(miraPulso * 4f + a.x) * 3f);

            shape.setColor(new Color(0.60f + 0.30f * urgencia, 0.10f, 0.85f - 0.50f * urgencia, 0.30f));
            shape.circle(a.x, a.y, a.raio + 5f);

            shape.setColor(new Color(0.28f + 0.42f * urgencia, 0.06f, 0.72f - 0.42f * urgencia, 0.92f));
            shape.circle(a.x, a.y, a.raio);

            shape.setColor(new Color(1f, 1f, 1f, 0.12f));
            shape.circle(a.x, a.y, a.raio * 0.72f);
            shape.setColor(new Color(1f, 1f, 1f, 0.85f));
            shape.circle(a.x, a.y, a.raio * 0.18f);

            float bvX = a.x - a.raio, bvY = a.y - a.raio - 8f, bvLarg = a.raio * 2f;
            shape.setColor(new Color(0.15f, 0.05f, 0.25f, 0.7f));
            shape.rect(bvX, bvY, bvLarg, 4f);
            shape.setColor(new Color(0.15f + 0.7f * urgencia, 0.75f - 0.65f * urgencia, 0.20f, 0.9f));
            shape.rect(bvX, bvY, bvLarg * propVida, 4f);
        }

        boolean sobreAlvo = false;
        for (Alvo a : alvos) {
            if (new Vector2(miraX - a.x, miraY - a.y).len() <= a.raio + RAIO_MIRA) { sobreAlvo = true; break; }
        }
        Color corMira     = sobreAlvo ? new Color(0.15f, 0.95f, 0.45f, 1f)    : new Color(0.85f, 0.85f, 1.00f, 1f);
        Color corMiraHalo = sobreAlvo ? new Color(0.15f, 0.95f, 0.45f, 0.12f) : new Color(0.55f, 0.20f, 0.90f, 0.10f);
        float pulsoRaio   = RAIO_MIRA + 11f + MathUtils.sin(miraPulso * 3.2f) * 2f;
        float escR        = miraEscala;

        shape.setColor(corMiraHalo);
        shape.circle(miraX, miraY, (pulsoRaio + 14f) * escR);

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);

        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 0.4f));
        shape.rect(10, ALTURA - MARGEM_HUD + 8f, 130f, 44f);
        shape.setColor(new Color(0.15f, 0.75f, 0.40f, 0.5f));
        shape.rect(LARGURA - 265f, ALTURA - MARGEM_HUD + 8f, 120f, 44f);
        shape.setColor(new Color(0.85f, 0.20f, 0.20f, 0.5f));
        shape.rect(LARGURA - 135f, ALTURA - MARGEM_HUD + 8f, 80f, 44f);

        shape.setColor(new Color(0.30f, 0.10f, 0.50f, 0.4f));
        shape.line(10f, ALTURA - MARGEM_HUD + 5f, LARGURA - 10f, ALTURA - MARGEM_HUD + 5f);

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

        float diagR = RAIO_MIRA * 0.55f * escR, diagLen = 5f * escR;
        for (int q = 0; q < 4; q++) {
            float ang = MathUtils.degreesToRadians * (45f + q * 90f);
            float cx  = miraX + MathUtils.cos(ang) * diagR;
            float cy  = miraY + MathUtils.sin(ang) * diagR;
            shape.line(cx, cy,
                    miraX + MathUtils.cos(ang) * (diagR + diagLen),
                    miraY + MathUtils.sin(ang) * (diagR + diagLen));
        }

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(corMira);
        shape.circle(miraX, miraY, 3.5f * escR);

        Color corEstab = estabilidadeMedia > 0.6f
                ? new Color(0.20f, 0.85f, 0.45f, 0.7f)
                : new Color(0.90f, 0.25f, 0.25f, 0.7f);
        shape.setColor(corEstab);
        shape.rect(0, ALTURA - MARGEM_HUD, LARGURA * estabilidadeMedia, 3f);
        shape.end();

        batch.begin();

        int     seg           = (int) tempoRestante;
        boolean urgenciaTimer = tempoRestante < 10f;
        font.getData().setScale(1.3f);
        font.setColor(urgenciaTimer ? new Color(1f, 0.35f, 0.35f, 1f) : new Color(0.85f, 0.70f, 1.00f, 1f));
        font.draw(batch, "TEMPO", 20, ALTURA - MARGEM_HUD + 48f);
        font.getData().setScale(1.8f);
        font.setColor(urgenciaTimer ? new Color(1f, 0.25f, 0.25f, 1f) : Color.WHITE);
        font.draw(batch, String.format("%02d:%02d", seg / 60, seg % 60), 20, ALTURA - MARGEM_HUD + 32f);

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
        font.draw(batch, "ESTAB: " + labelEstab + "  (" + (int)(estabilidadeMedia * 100) + "%)", 10, ALTURA - MARGEM_HUD - 4f);

        font.getData().setScale(0.85f);
        font.setColor(new Color(0.45f, 0.45f, 0.60f, 1f));
        font.draw(batch, "Controle Analogico  -  BOTAO para disparar  -  ESC: menu", 10, 14f);

        batch.end();
    }

    // =========================================================================
    // DESENHO – GAME OVER
    // =========================================================================
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

        // Verifica se o jogador bateu a meta para mudar os textos
        boolean passou = false;
        if (dificuldade == 1 && scoreReacao >= 75f) passou = true;
        else if (dificuldade == 2 && scoreReacao >= 50f) passou = true;
        else if (dificuldade == 3 && scoreReacao >= 30f) passou = true;

        font.getData().setScale(2.6f);
        if (passou) {
            font.setColor(new Color(0.20f, 0.85f, 0.45f, 1f)); // Verde
            font.draw(batch, "FASE " + dificuldade + " CONCLUIDA!", 105, ALTURA - 90f);
        } else {
            font.setColor(new Color(0.90f, 0.25f, 0.25f, 1f)); // Vermelho
            font.draw(batch, "TREINO FALHOU. TENTE NOVAMENTE.", 85, ALTURA - 90f);
        }

        // Subimos um pouco os textos porque a fórmula foi removida
        float linhaY = ALTURA - 150f; 
        font.getData().setScale(1.3f);
        font.setColor(new Color(0.55f, 0.75f, 1.00f, 1f));
        font.draw(batch, "Precisao  (P)", 110f, linhaY);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f%%", scorePrecisao), 530f, linhaY);

        linhaY -= 35f;
        font.setColor(new Color(0.30f, 1.00f, 0.60f, 1f));
        font.draw(batch, "Estabilidade  (E)", 110f, linhaY);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f%%", scoreEstabilidade), 530f, linhaY);

        linhaY -= 35f;
        
        // Exibe a porcentagem de acertos e a meta necessária
        font.setColor(new Color(1.00f, 0.75f, 0.30f, 1f));
        font.draw(batch, "Acertos  (T)", 110f, linhaY);
        font.setColor(Color.WHITE);
        
        int meta = (dificuldade == 1) ? 75 : (dificuldade == 2) ? 50 : 30;
        font.draw(batch, String.format("%.0f%%  (Meta: %d%%)", scoreReacao, meta), 470f, linhaY);

        font.getData().setScale(2.2f);
        font.setColor(new Color(0.75f, 0.45f, 1.00f, 1f));
        font.draw(batch, "SCORE FINAL", 115, 160f);
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f", scoreFinal), 460f, 160f);

        // Texto do rodapé dinâmico dependendo se passou ou não
        font.getData().setScale(1.1f);
        font.setColor(new Color(0.55f, 0.55f, 0.70f, 1f));
        String textoRodape;
        if (passou) {
            textoRodape = (dificuldade < 3) ? "ENTER/BOTAO: proxima fase    ESC: menu" : "ENTER/BOTAO: concluir treino    ESC: menu";
        } else {
            textoRodape = "ENTER/BOTAO: repetir fase    ESC: menu";
        }
        font.draw(batch, textoRodape, 130f, 88f);

        batch.end();
    
    }

    // =========================================================================
    // LÓGICA E DESENHO – PAUSE
    // =========================================================================
    private void atualizarPause() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            opcaoPausaSelecionada = (opcaoPausaSelecionada + 1) % opcoesPausa.length;
            
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
            opcaoPausaSelecionada = (opcaoPausaSelecionada - 1 + opcoesPausa.length) % opcoesPausa.length;

        boolean apertouEnter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
                              (joystick != null && joystick.botaoPressionado && !botaoAnterior);
                              
        if (joystick != null) botaoAnterior = joystick.botaoPressionado;

        if (apertouEnter) {
            if (joystick != null) joystick.botaoPressionado = false;
            
            switch (opcaoPausaSelecionada) {
                case 0: // CONTINUAR
                    estado = Estado.JOGANDO;
                    if (musicaFundo != null) musicaFundo.play();
                    Gdx.input.setCursorCatched(true);
                    break;
                case 1: // REINICIAR FASE
                    if (musicaFundo != null) musicaFundo.stop();
                    game.setScreen(new GameScreen(game, dificuldade));
                    break;
                case 2: // VOLTAR AO MENU
                    if (musicaFundo != null) musicaFundo.stop();
                    game.setScreen(new MenuScreen(game));
                    break;
            }
        }
    }

    private void desenharPause() {
        // Fundo escurecido semi-transparente
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0, 0, 0, 0.75f));
        shape.rect(0, 0, LARGURA, ALTURA);
        
        // Caixa do menu
        shape.setColor(new Color(0.1f, 0.05f, 0.2f, 0.9f));
        shape.rect(LARGURA / 2 - 150, ALTURA / 2 - 100, 300, 200);
        shape.setColor(new Color(0.55f, 0.20f, 0.90f, 1f));
        shape.rect(LARGURA / 2 - 150, ALTURA / 2 - 100, 4, 200);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(2f);
        font.setColor(Color.CYAN);
        font.draw(batch, "SISTEMA PAUSADO", 0, ALTURA / 2 + 70, LARGURA, Align.center, false);
        
        font.getData().setScale(1.2f);
        for (int i = 0; i < opcoesPausa.length; i++) {
            if (i == opcaoPausaSelecionada) {
                font.setColor(Color.YELLOW);
                font.draw(batch, "> " + opcoesPausa[i], LARGURA / 2 - 80, ALTURA / 2 + 10 - (i * 40));
            } else {
                font.setColor(Color.WHITE);
                font.draw(batch, opcoesPausa[i], LARGURA / 2 - 60, ALTURA / 2 + 10 - (i * 40));
            }
        }
        batch.end();
    }

    @Override public void pause()  {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        if (joystick != null) joystick.fechar();
        if (musicaFundo != null) musicaFundo.stop(); // Para a música ao sair da tela
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
        if (joystick != null) joystick.fechar();
        if (musicaFundo != null) musicaFundo.dispose(); // Limpa a memória
    }
}