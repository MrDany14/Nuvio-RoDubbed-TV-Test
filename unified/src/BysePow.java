import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Independent translation of the public player's gr/ye/wr functions.
 * Android/Java int arithmetic supplies the required modulo-2^32 operations.
 * No networking, plugin code, or credentials are included in this benchmark.
 */
public final class BysePow {
    private final int[] state = new int[4];
    private final int[] table = new int[512];
    private final int[] output = new int[8];

    private static void quarterRound(int[] s) {
        s[0] += s[1]; s[3] = Integer.rotateLeft(s[3] ^ s[0], 16);
        s[2] += s[3]; s[1] = Integer.rotateLeft(s[1] ^ s[2], 12);
        s[0] += s[1]; s[3] = Integer.rotateLeft(s[3] ^ s[0], 8);
        s[2] += s[3]; s[1] = Integer.rotateLeft(s[1] ^ s[2], 7);
    }

    public int[] hash(byte[] bytes, int length, boolean firstOnly) {
        int[] s = state;
        s[0]=1779033703; s[1]=(int)3144134277L;
        s[2]=1013904242; s[3]=(int)2773480762L;
        for (int i=0;i<length;i++) {
            s[0]=Integer.rotateLeft(s[0]+(bytes[i]&255),7);
            quarterRound(s);
        }
        for(int i=0;i<8;i++) quarterRound(s);
        for(int i=0;i<512;i++) { quarterRound(s); table[i]=s[0]^s[2]; }
        for(int pass=0;pass<2;pass++) {
            for(int i=0;i<512;i++) {
                int v=Integer.rotateLeft(table[i]+table[table[i]&511],13);
                v ^= table[(i+1)&511] * (int)2654435761L;
                table[i]=v; s[0]^=v; quarterRound(s);
            }
        }
        for(int i=0;i<(firstOnly?1:8);i++) {
            quarterRound(s);
            int sum=s[0];
            for(int j=i*64;j<(i+1)*64;j++) {
                int v=table[j];
                sum=Integer.rotateLeft(sum+v,5) ^ (v*(int)2246822519L);
            }
            output[i]=sum^s[2];
        }
        return output;
    }

    public static void main(String[] args) throws Exception {
        BysePow pow=new BysePow();
        if(args.length>0 && args[0].equals("vectors")) {
            int count=0;
            for(String line:Files.readAllLines(Paths.get(args[1]),StandardCharsets.US_ASCII)) {
                String[] parts=line.split(" ");
                byte[] input=new byte[parts[0].length()/2];
                for(int i=0;i<input.length;i++)input[i]=(byte)Integer.parseInt(parts[0].substring(2*i,2*i+2),16);
                int[] result=pow.hash(input,input.length,false);
                StringBuilder actual=new StringBuilder();
                for(int v:result)actual.append(String.format("%08x",v));
                if(!actual.toString().equals(parts[1]))throw new AssertionError("Frontend vector mismatch "+count);
                int first=result[0];
                if(pow.hash(input,input.length,true)[0]!=first)throw new AssertionError("First-only mismatch");
                count++;
            }
            System.out.println("PASS "+count+" public-frontend vectors, full and first-word hash");
            return;
        }
        byte[] input="0123456789abcdef0123456789abcdef:000000".getBytes(StandardCharsets.US_ASCII);
        long sink=0;
        for(int trial=0;trial<4;trial++) {
            int attempts=trial==0?1000:10000;
            long start=System.nanoTime();
            for(int i=0;i<attempts;i++) { input[input.length-1]=(byte)(i&255); sink^=pow.hash(input,input.length,true)[0]; }
            double ms=(System.nanoTime()-start)/1e6;
            System.out.printf(java.util.Locale.ROOT,"NATIVE trial=%d attempts=%d ms=%.1f hashesPerSecond=%.0f sink=%d%n",trial,attempts,ms,attempts*1000/ms,sink);
        }
    }
}
