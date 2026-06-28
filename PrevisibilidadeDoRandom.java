import java.util.Random;

/**
 * Demonstra por que java.util.Random NAO serve para segredos:
 *  (1) determinismo — mesma seed, mesma sequencia;
 *  (2) recuperacao do estado de 48 bits a partir de 2 saidas e previsao da 3a.
 *
 * Rode com Java 17+ :  java src/PrevisibilidadeDoRandom.java
 */
public class PrevisibilidadeDoRandom {

    static final long MULT = 0x5DEECE66DL, ADD = 0xBL, MASK = (1L << 48) - 1;

    public static void main(String[] args) {
        determinismo();
        ataque();
    }

    /** Duas instancias com a mesma seed produzem sequencias identicas. */
    static void determinismo() {
        System.out.println("== Determinismo (seed = 42) ==");
        Random a = new Random(42), b = new Random(42);
        for (int i = 0; i < 3; i++)
            System.out.printf("a=%d  b=%d%n", a.nextInt(), b.nextInt());
    }

    /** Observa 2 saidas, recupera o estado e preve a 3a antes de ela existir. */
    static void ataque() {
        System.out.println("\n== Quebra do java.util.Random ==");
        Random victim = new Random();              // seed desconhecida
        int o1 = victim.nextInt();                 // so observo a saida
        int o2 = victim.nextInt();
        System.out.printf("observado o1=%d%nobservado o2=%d%n", o1, o2);

        long recovered = -1;
        long highBits = ((long) o1 & 0xFFFFFFFFL) << 16;   // 32 bits altos do estado
        for (long low = 0; low < (1 << 16); low++) {       // 16 bits que faltam
            long seed1 = highBits | low;
            long seed2 = (seed1 * MULT + ADD) & MASK;
            if ((int) (seed2 >>> 16) == o2) { recovered = seed2; break; }
        }
        if (recovered < 0) { System.out.println("nao recuperou"); return; }

        int previsto = (int) (((recovered * MULT + ADD) & MASK) >>> 16);
        int real = victim.nextInt();               // so agora o 3o numero existe
        System.out.printf("PREVISTO o3=%d%nREAL     o3=%d%nacerto=%b%n",
                previsto, real, previsto == real);
    }
}
