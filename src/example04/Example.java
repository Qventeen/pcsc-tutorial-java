package example04;

/*
 * Copyright (c) 2019, Sergey Stolyarov <sergei@regolit.com>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import javax.smartcardio.*;
import static java.util.Arrays.copyOfRange;
import java.util.ArrayList;

class Example {
    public static class TerminalNotFoundException extends Exception {}
    public static class InstructionFailedException extends Exception {}

    public static void main(String[] args) {
        try {
            var factory = TerminalFactory.getDefault();
            var terminals = factory.terminals().list();

            if (terminals.size() == 0) {
                throw new TerminalNotFoundException();
            }

            // get first terminal
            var terminal = terminals.get(0);

            System.out.printf("Using terminal %s%n", terminal.toString());

            // wait for card, indefinitely until card appears
            terminal.waitForCardPresent(0);

            // establish a connection to the card using autoselected protocol
            var card = terminal.connect("*");

            // obtain logical channel
            var channel = card.getBasicChannel();
            ResponseAPDU answer;
            byte[] command;

            // 1. load key
            //                     CLA  INS  P1  P2  Lc  Data
            command = toByteArray("FF   82   00  00  06  FF FF FF FF FF FF");
            answer = channel.transmit(new CommandAPDU(command));
            if (answer.getSW() != 0x9000) {
                System.out.printf("Response failed: %s%n", hexify(answer.getBytes()));
                throw new InstructionFailedException();
            }

            // 2. authentication
            //                     CLA  INS  P1  P2  Lc  Data
            command = toByteArray("FF   86   00  00  05  01 00 00 60 00");
            answer = channel.transmit(new CommandAPDU(command));
            if (answer.getSW() != 0x9000) {
                System.out.printf("Response failed: %s%n", hexify(answer.getBytes()));
                throw new InstructionFailedException();
            }

            // 3. read data
            //                     CLA  INS  P1  P2  Le
            command = toByteArray("FF   B0   00  00  10");
            answer = channel.transmit(new CommandAPDU(command));
            if (answer.getSW() != 0x9000) {
                System.out.printf("Response failed: %s%n", hexify(answer.getBytes()));
                throw new InstructionFailedException();
            }
            System.out.printf("Block data: %s%n", hexify(answer.getData()));

            // disconnect card
            card.disconnect(false);

        } catch (InstructionFailedException e) {
            System.out.println("Instruction execution failed.");
        } catch (TerminalNotFoundException e) {
            System.out.println("No connected terminals.");
        } catch (CardException e) {
            System.out.println("CardException: " + e.toString());
        }
    }

    public static String hexify(byte[] bytes) {
        var bytesStrings = new ArrayList<String>(bytes.length);
        for (var b : bytes) {
            bytesStrings.add(String.format("%02X", b));
        }
        return String.join(" ", bytesStrings);
    }

    public static byte[] toByteArray(String s) {
        var len = s.length();
        var buf = new byte[len/2];
        int bufLen = 0;
        int i = 0;
        
        while (i < len) {
            var c1 = s.charAt(i);
            i++;
            if (c1 == ' ') {
                continue;
            }
            var c2 = s.charAt(i);
            i++;

            byte d = (byte)((Character.digit(c1, 16) << 4) + (Character.digit(c2, 16)));
            buf[bufLen] = d;
            ++bufLen;
        }

        return copyOfRange(buf, 0, bufLen);
    }
}
