package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

import mack.game.JoystickInput;

public class StoryScreen implements Screen {
    private Game game;
    private SpriteBatch batch;
    private BitmapFont fontText;
    private BitmapFont fontTitle;
    private String storyText;
    private String titleText;
    private int faseHistoria;
    
    private JoystickInput joystick;
    private Thread joystickThread;
    private boolean botaoAnterior = false;

    // Construtor agora recebe qual "Capítulo" da história estamos
    public StoryScreen(Game game, int faseHistoria) {
        this.game = game;
        this.faseHistoria = faseHistoria;
        this.batch = new SpriteBatch();
        
        this.fontTitle = new BitmapFont();
        this.fontTitle.getData().setScale(2.5f);
        this.fontText = new BitmapFont();
        this.fontText.getData().setScale(1.5f);
        
        this.joystick = new JoystickInput("COM4"); 
        this.joystickThread = new Thread(joystick);
        this.joystickThread.setDaemon(true);
        this.joystickThread.start();

        // --- HISTÓRIAS DE CADA FASE ---
        switch (faseHistoria) {
            case 0: // Introdução (Veio do Menu)
                titleText = "PROJETO NeuroAim";
                storyText = "Em um futuro proximo, cientistas descobriram uma dimensao chamada Neurospace, \n" +
                            "onde as conexoes neurais humanas sao representadas digitalmente.\n\n" +
                            "Uma falha comecou a corromper essas conexoes, causando perda de controle motor, \n" +
                            "falhas cognitivas e instabilidade nos movimentos.\n\n" +
                            "Para resolver isso, foi criado o NeuroAim, um sistema que permite ao usuario acessar \n" +
                            "o Neurospace e restaurar essas conexoes por meio de acoes precisas.\n\n" +
                            "Voce e um Operador Neural. Elimine as falhas e recupere as conexoes.\n" +
                            "O controle exige precisao, estabilidade e foco.";
                break;
                
            case 1: // Passou a Fase 1
                titleText = "SISTEMA MOTOR BASICO ONLINE";
                storyText = "Bom trabalho, Operador. As conexoes primarias foram restabelecidas.\n\n" +
                            "No entanto, o sistema imunologico corrompido do Neurospace detectou sua presenca.\n" +
                            "As falhas agora estao se movendo mais rapido para evitar a estabilizacao.\n\n" +
                            "Prepare-se para movimentos mais agudos.\n" +
                            "Iniciando Nivel 2...";
                break;
                
            case 2: // Passou a Fase 2
                titleText = "ESTABILIDADE PARCIAL ALCANCADA";
                storyText = "Apenas o nucleo neural profundo permanece corrompido.\n\n" +
                            "Esta e a raiz da instabilidade. As anomalias estao totalmente erraticas e velozes.\n" +
                            "Esta e a ultima etapa. Exigimos precisao cirurgica do seu controle motor.\n\n" +
                            "Mantenha o foco absoluto, Operador.\n" +
                            "Iniciando Sequencia Final...";
                break;
                
            case 3: // Passou a Fase 3 (FINAL DO JOGO)
                titleText = "SISTEMA TOTALMENTE RESTAURADO";
                storyText = "TREINAMENTO CONCLUIDO COM SUCESSO!\n\n" +
                            "Voce eliminou as falhas mais complexas do Neurospace.\n" +
                            "As conexoes neurais foram estabilizadas e o controle motor do paciente\n" +
                            "foi 100% restaurado ao mundo real.\n\n" +
                            "Obrigado por jogar NeuroAim.";
                break;
        }
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        boolean botaoAtual = joystick.botaoPressionado;
        boolean clicouJoystick = botaoAtual && !botaoAnterior;
        botaoAnterior = botaoAtual;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
            clicouJoystick) {
            
            if (faseHistoria < 3) {
                // Se ainda tem jogo pela frente, vai pra próxima fase
                game.setScreen(new GameScreen(game, faseHistoria + 1));
            } else {
                // Se for a fase 3 (história final), volta pro menu
                game.setScreen(new MenuScreen(game));
            }
            dispose();
            return;
        }

        batch.begin();
        
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, titleText, 0, Gdx.graphics.getHeight() - 50, Gdx.graphics.getWidth(), Align.center, false);

        fontText.setColor(Color.WHITE);
        fontText.draw(batch, storyText, 100, Gdx.graphics.getHeight() - 150, Gdx.graphics.getWidth() - 200, Align.center, true);

        fontText.setColor(Color.YELLOW);
        String botaoTexto = (faseHistoria < 3) ? "Pressione o Botao ou ESPACO para continuar..." : "Pressione o Botao ou ESPACO para voltar ao Menu";
        fontText.draw(batch, botaoTexto, 0, 80, Gdx.graphics.getWidth(), Align.center, false);
        
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        if (joystick != null) joystick.fechar();
    }

    @Override
    public void dispose() {
        batch.dispose();
        fontText.dispose();
        fontTitle.dispose();
        if (joystick != null) joystick.fechar();
    }
}