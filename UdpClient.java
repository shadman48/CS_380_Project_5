import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class UdpClient {
	public static void main(String[] args) throws IOException {
		try (Socket socket = new Socket("18.221.102.182", 38005)) {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			double sent, recived, avg = 0, RTT;

			byte[] message = { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
			os.write(Ipv4_Part(4, message));
			System.out.print("Handshake response: ");
			Reply(is);
			System.out.println();
			byte[] portArray = new byte[2];
			portArray[0] = (byte) is.read();
			portArray[1] = (byte) is.read();
			int port = ((portArray[0] & 0xFF) << 8) | (portArray[1] & 0xFF);

			System.out.println("Port number received: " + port + "\n");
			int size = 1;
			for (int i = 0; i < 12; ++i) {
				size <<= 1;
				byte[] data = new byte[size];
				Random rand = new Random();
				for (int j = 0; j < size; ++j) {
					data[i] = (byte) rand.nextInt(256);
				}
				System.out.println("Sending packet with " + size + " bytes of data");

				int length = 8 + data.length;
				byte[] packet = new byte[length];
				packet[0] = (byte) 0xFF;
				packet[1] = (byte) 0xFF;
				packet[2] = (byte) ((port & 0xFF00) >>> 8);
				packet[3] = (byte) (port & 0x00FF);
				packet[4] = (byte) (length >> 8);
				packet[5] = (byte) length;
				short checksum = UDP_Header(length, port, data);
				packet[6] = (byte) ((checksum >> 8) & 0xFF);
				packet[7] = (byte) (checksum & 0xFF);
				for (int j = 8; j < length; ++j) {
					packet[j] = data[j - 8];
				}
				os.write(Ipv4_Part(size + 8, packet));
				sent = System.currentTimeMillis();
				System.out.print("Response: ");
				Reply(is);
				System.out.println();
				recived = System.currentTimeMillis();
				RTT = recived - sent;
				System.out.println("RTT: " + RTT + "ms\n");
				avg += RTT;
			}
			System.out.println("Average RTT: " + (avg / 12) + "ms");
		}
	}

	
	
	
	
	
	public static void Reply(InputStream is) throws IOException {
		System.out.print("0x");
		byte fromServer;
		for (int i = 0; i < 4; ++i) {
			fromServer = (byte) is.read();
			System.out.print(Integer.toHexString(fromServer & 0xFF).toUpperCase());
		}
	}

	
	
	
	
	
	
	
	public static byte[] Ipv4_Part(int size, byte[] data) {
		short length = (short) (20 + size);
		byte[] packet = new byte[length];
		packet[0] = 0x45;
		packet[1] = 0x0;
		packet[2] = (byte) ((length >> 8) & 0xFF);
		packet[3] = (byte) (length & 0xFF);
		packet[4] = 0x0;
		packet[5] = 0x0;
		packet[6] = (0x1 << 6);
		packet[7] = 0x0;
		packet[8] = 0x32;
		packet[9] = 0x11;
		packet[10] = 0x0;
		packet[11] = 0x0;
		packet[12] = (byte) 13;
		packet[13] = (byte) 208;
		packet[14] = (byte) 14;
		packet[15] = (byte) 72;
		packet[16] = (byte) 18;
		packet[17] = (byte) 221;
		packet[18] = (byte) 102;
		packet[19] = (byte) 182;

		short checksum = checksum(packet);
		packet[10] = (byte) ((checksum >> 8) & 0xFF);
		packet[11] = (byte) (checksum & 0xFF);

		for (int i = 20; i < 20 + data.length; ++i) {
			packet[i] = data[i - 20];
		}
		return packet;
	}
	
	
	
	
	
	
	
	
	
	
	
	public static short UDP_Header(int size, int port, byte[] data) {
		int length = 20 + size;
		byte[] packet = new byte[length];
		packet[0] = (byte) 13;
		packet[1] = (byte) 208;
		packet[2] = (byte) 14;
		packet[3] = (byte) 72;
		packet[4] = (byte) 18;
		packet[5] = (byte) 221;
		packet[6] = (byte) 102;
		packet[7] = (byte) 182;
		packet[8] = 0x0;
		packet[9] = 0x11;
		packet[10] = (byte) (size >> 8);
		packet[11] = (byte) (size & 0xFF);
		packet[12] = (byte) 0xFF;
		packet[13] = (byte) 0xFF;
		packet[14] = (byte) ((port & 0xFF00) >>> 8);
		packet[15] = (byte) (port & 0x00FF);
		packet[16] = (byte) (size >> 8);
		packet[17] = (byte) (size & 0xFF);

		for (int i = 0; i < data.length; ++i) {
			packet[i + 18] = data[i];
		}
		return checksum(packet);
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	public static short checksum(byte[] bytes) {
		int length = bytes.length;
		int index = 0;
		long sum = 0;
		while (length > 1) {
			sum += (((bytes[index] << 8) & 0xFF00) | ((bytes[index + 1]) & 0xFF));
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
			index += 2;
			length -= 2;
		}
		if (length > 0) {
			sum += (bytes[index] << 8 & 0xFF00);
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}
		sum = sum & 0xFFFF;
		return (short) ~sum;
	}
}