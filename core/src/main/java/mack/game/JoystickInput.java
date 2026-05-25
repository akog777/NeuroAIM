package mack.game;

import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

public class JoystickInput implements Runnable {
    private SerialPort porta;
    private boolean rodando = true;

    // O 'volatile' garante que o jogo sempre veja o valor mais recente
    public volatile int x = 2048;
    public volatile int y = 2048;
    public volatile boolean botaoPressionado = false;

    public JoystickInput(String nomePorta) { 
        try {
            porta = SerialPort.getCommPort(nomePorta);
            porta.setBaudRate(115200);
            // Configura a porta para não travar o Java
            porta.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            
            if (porta.openPort()) {
                System.out.println("SUCESSO: Conectado ao ESP32 na porta " + nomePorta);
            } else {
                System.err.println("ERRO: Nao foi possivel abrir a porta " + nomePorta);
            }
        } catch (Exception e) {
            System.err.println("Erro na porta serial: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        if (porta == null || !porta.isOpen()) return;
        
        InputStream in = porta.getInputStream();
        StringBuilder buffer = new StringBuilder();
        
        while (rodando) {
            try {
                // Lê os dados brutos sem usar o Scanner (muito mais rápido e não trava)
                if (in.available() > 0) {
                    char c = (char) in.read();
                    if (c == '\n') { // Fim da linha enviada pelo ESP32
                        String linha = buffer.toString().trim();
                        buffer.setLength(0); // Limpa o buffer para a próxima leitura
                        
                        String[] dados = linha.split(",");
                        if (dados.length == 3) {
                            this.x = Integer.parseInt(dados[0].trim());
                            this.y = Integer.parseInt(dados[1].trim());
                            this.botaoPressionado = Integer.parseInt(dados[2].trim()) == 0;
                        }
                    } else {
                        buffer.append(c);
                    }
                } else {
                    // Pausa minúscula (2 milissegundos) para não fritar o processador
                    Thread.sleep(2); 
                }
            } catch (Exception e) {
                // Se o cabo der mau contato e vier "lixo", limpa e tenta de novo sem matar o código
                buffer.setLength(0);
            }
        }
    }

    public void fechar() {
        rodando = false;
        if (porta != null && porta.isOpen()) {
            porta.closePort();
            System.out.println("Porta serial fechada com segurança.");
        }
    }
}