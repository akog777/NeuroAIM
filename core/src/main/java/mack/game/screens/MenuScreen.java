package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MenuScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;
    private static final float V_WIDTH  = 800f;
    private static final float V_HEIGHT = 480f;

    private static int dificuldade = 1;

    private String[] opcoes = { "JOGAR", "CONFIGURAÇÕES", "TUTORIAL", "SAIR" };
    private int opcaoSelecionada = 0;

    public MenuScreen(Game game) {
        this.game = game;
    }

    public static void setDificuldade(int d) { dificuldade = d; }
    public static int  getDificuldade()      { return dificuldade; }

    @Override
    public void show() {
        // --- CONFIGURAÇÃO DA CÂMERA (TELA ESTICADA - SEM BORDAS) ---
        camera   = new OrthographicCamera();
        viewport = new StretchViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply();
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);

        // --- INICIALIZAÇÃO DE RECURSOS ---
        batch = new SpriteBatch();
        font  = new BitmapFont();
        background = new Texture(Gdx.files.internal("menu.jpeg")); 
    }

    @Override
    public void render(float delta) {
        // --- ATUALIZAÇÃO DA CÂMERA E ENTRADAS ---
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        verificarEntrada();

        // --- LIMPEZA DE TELA ---
        Gdx.gl.glClearColor(0.95f, 0.93f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- RENDERIZAÇÃO DOS ELEMENTOS VISUAIS ---
        batch.begin();
        
        batch.draw(background, 0, 0, V_WIDTH, V_HEIGHT);

        font.getData().setScale(2f);
        for (int i = 0; i < opcoes.length; i++) {
            if (i == opcaoSelecionada) {
                font.setColor(Color.YELLOW); 
                font.draw(batch, "> " + opcoes[i], 50, 300 - i * 55); 
            } else {
                font.setColor(Color.WHITE); 
                font.draw(batch, opcoes[i], 85, 300 - i * 55); 
            }
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, "Use W/S ou Setas para navegar | ENTER para selecionar", 50, 60);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    // --- LÓGICA DE NAVEGAÇÃO DE MENU ---
    private void verificarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            opcaoSelecionada = (opcaoSelecionada + 1) % opcoes.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            opcaoSelecionada = (opcaoSelecionada - 1 + opcoes.length) % opcoes.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            selecionarOpcao();
    }

    // --- AÇÕES DO MENU ---
    private void selecionarOpcao() {
        switch (opcaoSelecionada) {
            case 0: game.setScreen(new StoryScreen(game, 0));          break;
            case 1: game.setScreen(new ConfigScreen(game));            break;
            case 2: game.setScreen(new TutorialScreen(game));          break;
            case 3: Gdx.app.exit();                                    break;
        }
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    // --- LIBERAÇÃO DE MEMÓRIA (PREVENÇÃO DE VAZAMENTOS) ---
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        background.dispose();
    }
}