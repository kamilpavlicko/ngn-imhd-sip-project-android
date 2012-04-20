package sk.fiit.stuba.androidimhd;

import java.security.MessageDigest;

public class MD5HashingExample 
{
    public static String computeDigest(String string) throws Exception
    { 
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(string.getBytes());
 
        byte byteData[] = md.digest();
 
        //convert the byte to hex format method 1
        /* not used
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
 		
        System.out.println("Digest(in hex format) 1st method:: " + sb.toString());
        */
 
        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<byteData.length;i++) {
    		String hex=Integer.toHexString(0xff & byteData[i]);
   	     	if(hex.length()==1) hexString.append('0');
   	     	hexString.append(hex);
    	}
    	System.out.println("Digest(in hex format) 2nd method:: " + hexString.toString());
    	return hexString.toString();
    }
}
