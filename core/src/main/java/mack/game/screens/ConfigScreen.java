package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;
    private static final float V_WIDTH  = 800f;
    private static final float V_HEIGHT = 480f;

    private boolean somLigado  = true;
    private int     dificuldade = MenuScreen.getDificuldade();

    // --- VARIÁVEL GLOBAL DA PORTA COM (AGORA AUTOMÁTICA) ---
    public static String portaCOM = autoDetectarPorta(); 

    private static String autoDetectarPorta() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length > 0) {
            return ports[0].getSystemPortName(); // Pega a primeira USB que encontrar conectada!
        }
        return "COM4"; // Padrão de segurança se nada for encontrado
    }
    
    // --- LÓGICA DE DETECÇÃO DE PORTAS ---
    private List<String> portasDisponiveis;
    private int portaIndex = 0;

    public ConfigScreen(Game game) {
        this.game = game;
        carregarPortas();
    }

    // --- BUSCA AS PORTAS CONECTADAS NO PC ---
    private void carregarPortas() {
        portasDisponiveis = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        
        for (SerialPort port : ports) {
            portasDisponiveis.add(port.getSystemPortName());
        }
        
        if (portasDisponiveis.isEmpty()) {
            portasDisponiveis.add("NENHUMA");
        } else {
            portaIndex = portasDisponiveis.indexOf(portaCOM);
            if (portaIndex == -1) {
                portaIndex = 0; 
                portaCOM = portasDisponiveis.get(0);
            }
        }
    }

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply();
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        font  = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        verificarEntrada();

        Gdx.gl.glClearColor(0.96f, 0.94f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(new Color(0.25f, 0.10f, 0.50f, 1));
        font.getData().setScale(2.5f);
        font.draw(batch, "CONFIGURACOES", 200, 410);

        font.getData().setScale(1.5f);
        font.setColor(Color.DARK_GRAY);
        font.draw(batch, "Som: " + (somLigado ? "Ligado" : "Desligado"), 230, 320);
        font.draw(batch, "Dificuldade: " + getNomeDificuldade(), 230, 270);
        
        // --- MOSTRA A PORTA SELECIONADA ---
        font.setColor(new Color(0.2f, 0.5f, 0.8f, 1));
        font.draw(batch, "Porta do Controle: " + portaCOM, 230, 220);

        font.getData().setScale(1f);
        font.setColor(Color.GRAY);
        font.draw(batch, "Pressione ESPACO para ligar/desligar som", 150, 150);
        font.draw(batch, "Pressione D para mudar dificuldade",       170, 120);
        font.draw(batch, "Pressione P para trocar a porta COM",      165,  90);
        font.draw(batch, "Pressione ESC para voltar",                220,  60);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private void verificarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            somLigado = !somLigado;

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            dificuldade = (dificuldade % 3) + 1;
            MenuScreen.setDificuldade(dificuldade);
        }
        
        // --- TROCA A PORTA COM AO PRESSIONAR 'P' ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if (!portasDisponiveis.isEmpty() && !portasDisponiveis.get(0).equals("NENHUMA")) {
                portaIndex = (portaIndex + 1) % portasDisponiveis.size();
                portaCOM = portasDisponiveis.get(portaIndex);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    private String getNomeDificuldade() {
        switch (dificuldade) {
            case 1:  return "Inicial";
            case 2:  return "Intermediario";
            default: return "Avancado";
        }
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}