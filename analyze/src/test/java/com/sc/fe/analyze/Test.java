package com.sc.fe.analyze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sc.fe.analyze.util.CompareUtility;

public class Test {

    public static void main(String[] args) throws Exception {


        //DESCRIPTION --------
        /*Required types = signal, gerber or odb, drill
		
		availableType = solder_mask, drill
		availableFormat = gerber
		
		Stept 1- Remove all avaiable fileTypes from requiredTypes
		 - RequiredType = signal, gerber or odb
		Step 2 - Remove all availableFormat from requiredTypes
		 - RequiredType = signal, gerber or odb
		Step 3 -
		loop through each availableFormat {
		 - */
        
        //Required FileType is ---
        List<String> requiredTypes = new ArrayList<String>();
        requiredTypes.add("Signal");
        requiredTypes.add("Gerber or ODB");
        requiredTypes.add("Drill");

        //Available FileType is --
        List<String> availableTypes = new ArrayList<String>();
        availableTypes.add("silk_screen");
        availableTypes.add("drill");
        availableTypes.add("odb");
        
        System.out.println("requiredTypes Before:"+requiredTypes);
        
        List<String> missing = CompareUtility.findMissingItems(requiredTypes, availableTypes);
        // should return signal as missing
        System.out.println("missing :"+missing);
        System.out.println("requiredTypes After:"+requiredTypes);
        
        //========== Case 1
        availableTypes.clear();
        availableTypes.add("silk_screen");
        availableTypes.add("drill");
        availableTypes.add("ger");
        
        missing = CompareUtility.findMissingItems(requiredTypes, availableTypes);
        // Must return [signal, Gerber or ODB] as missing
        System.out.println("missing :"+missing);
        
        //========== Case 2
        availableTypes.clear();
        availableTypes.add("signal");
        availableTypes.add("drill");
        availableTypes.add("ger");
        
        missing = CompareUtility.findMissingItems(requiredTypes, availableTypes);
        // Must return [Gerber or ODB] as missing
        System.out.println("missing :"+missing);
    }
}
