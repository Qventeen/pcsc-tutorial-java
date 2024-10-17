package example03;

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
import java.util.ArrayList;

class Example {
    public static class TerminalNotFoundException extends Exception {}
    public static class InstructionFailedException extends Exception {}
    public static class ByteCastException extends Exception {}

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

            // execute command
            int[] command = {0xFF, 0xCA, 0x00, 0x00, 0x00};
            ResponseAPDU answer = channel.transmit(new CommandAPDU(toByteArray(command)));
            if (answer.getSW() != 0x9000) {
                throw new InstructionFailedException();
            }
            var uidBytes = answer.getData();
            System.out.printf("Card UID: %s%n", hexify(uidBytes));

            // disconnect card
            card.disconnect(false);

        } catch (TerminalNotFoundException e) {
            System.out.println("No connected terminals.");
        } catch (ByteCastException e) {
            System.out.println("Data casting error.");
        } catch (InstructionFailedException e) {
            System.out.println("Instruction execution failed.");
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

    public static byte[] toByteArray(int[] list)
        throws ByteCastException {
        var s = list.length;
        var buf = new byte[s];
        for (int i=0; i<s; i++) {
            if (i < 0 || i > 255) {
                throw new ByteCastException();
            }
            buf[i] = (byte)list[i];
        }
        return buf;
    }
}
