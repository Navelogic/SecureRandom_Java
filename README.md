# SecureRandom no Java

Código completo que acompanha o artigo **“SecureRandom: geração segura de valores aleatórios”**.

Os exemplos são **genéricos e ilustrativos**, escritos apenas para demonstrar os conceitos do artigo. Não representam nenhum sistema específico.

Testado em **OpenJDK 21** (Linux). Os dois arquivos de demonstração são *single-file* e rodam sem build:

```bash
java src/PrevisibilidadeDoRandom.java
java src/GeracaoSegura.java
```

---

## 1. Por que `java.util.Random` não serve para segredos

[`src/PrevisibilidadeDoRandom.java`](src/PrevisibilidadeDoRandom.java) demonstra duas coisas.

**Determinismo:** mesma seed, mesma sequência:

```
== Determinismo (seed = 42) ==
a=-1170105035  b=-1170105035
a=234785527    b=234785527
a=-1360544799  b=-1360544799
```

**A quebra:** observando 2 saídas, recupera-se o estado de 48 bits (só 16 bits são desconhecidos, `2^16 = 65.536` tentativas) e prevê-se a 3ª saída antes de ela existir:

```
== Quebra do java.util.Random ==
observado o1=750150095
observado o2=-1809524804
PREVISTO o3=365151497
REAL     o3=365151497
acerto=true
```

O mesmo vale para `Math.random()`, `ThreadLocalRandom` e o `RandomStringUtils` do Apache Commons no construtor padrão.

---

## 2. Gerando valores seguros com `SecureRandom`

[`src/GeracaoSegura.java`](src/GeracaoSegura.java) cobre token em hex, Base64 URL-safe, UUID v4 e DRBG:

```
algoritmo padrao = NativePRNG
codigo (hex)    = 4C0729AB1C01797A2EDE543135790635
codigo (base64) = Y79qc8Nr_uycmMuuxrq5Hw
uuid v4         = 569073c6-63ee-4fcf-8f3d-194997ae2788
algoritmo drbg  = DRBG
```

O coração da geração do token são três linhas:

```java
byte[] bytes = new byte[16];   // 128 bits
SECURE_RANDOM.nextBytes(bytes);
// ... codifica em hex ou Base64
```

---

## 3. O padrão dos dois identificadores (exemplo ilustrativo)

O exemplo conceitual do artigo: um serviço que gera **dois** identificadores com filosofias opostas e ambas corretas.

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OcorrenciaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OcorrenciaRepository repository;
    // ... demais dependências

    public OcorrenciaResponse criarOcorrencia(CriarOcorrenciaRequest request) {
        String protocolo = gerarProtocolo();                       // (1) sequencial
        String codigoAcompanhamento = gerarCodigoAcompanhamento();  // (2) secreto

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .protocolo(protocolo)
                .codigoAcompanhamento(codigoAcompanhamento)
                .anonima(request.isAnonima())
                // ...
                .build();

        return OcorrenciaResponse.of(repository.save(ocorrencia));
    }

    private String gerarProtocolo() {
        int ano = Year.now().getValue();
        long seq = repository.count() + 1;
        return "PSI-%d-%06d".formatted(ano, seq);                  // PSI-2025-000042
    }

    /** 16 bytes via SecureRandom -> 32 hex chars (128 bits de entropia). */
    private String gerarCodigoAcompanhamento() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
```

**Identificador 1: `protocolo` (`PSI-2025-000042`):** sequencial e previsível, mas toda consulta exige autorização. A segurança mora no controle de acesso:

```java
@Transactional(readOnly = true)
public OcorrenciaResponse buscarPorProtocolo(String protocolo) {
    validarPsicologaService.execute(currentUserService.getUsernameAtual());  // ← autorização
    return OcorrenciaResponse.of(
        repository.findByProtocolo(protocolo)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrencia", protocolo))
    );
}
```

**Identificador 2: `codigoAcompanhamento` (128 bits):** sem autorização nenhuma. O código **é** a credencial, então precisa ser impossível de adivinhar:

```java
@Transactional(readOnly = true)
public OcorrenciaResponse buscarPorCodigoAcompanhamento(String codigo) {
    return OcorrenciaResponse.of(                  // ← repare: NENHUMA verificação de perfil
        repository.findByCodigoAcompanhamento(codigo)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrencia", "código: " + codigo))
    );
}
```
