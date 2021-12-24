package com.marginallyclever.makelangelo.paper;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class PaperSize implements Comparable<Object>{
	public String name;
	public int width;
	public int height;
	
	PaperSize(String name,int width,int height) {
		this.name = name;
		this.width = width;
		this.height = height;
	}
	
	public String toString() {
		return name+" ("+width+" x "+height+")";
	}
	
	//
	// For the PaperTableModel needs ( interface comparable )
	//
	
//	
//    public String toString() {
//        return name + " (" + width + " x " + height + ")";//? String.format + Traductions
//    }
    
    // toString for the comparable + a string format -> ?

    @Override
    public int compareTo(Object o) {
        if (o != null) {
            if (o instanceof PaperSize) {
                PaperSize psO = (PaperSize)o;
                    
                // De là comment on organise ... prend t'on la surface comme 1er critére ?
                // 
                long oSurface = psO.height * psO.width;
                long thisSurface = height * width;
                
                if ( oSurface > thisSurface){
                    return 1;// Pour dire que l'on ordone du plus grand au plus petit ?
                }else if ( oSurface < thisSurface ){
                    return -1;
                }else{// { les deux on la meme surface }
                    // on pourais donc ici retourner 0 pour dire identique si seul la surface compte mais c'est pas le cas.
                    //donc il faut avoir un second critére ... la plus large en 1er ?

                    
                // Dans l'idée on utilise la plus grande dimension de taille du papier comme hauteur ?
                //donc sauf si carrer on va utiliser comme w la plus petite valeur.
                //int oMinDimAsW = Math.min(psO.height, psO.width); 
                int oMaxDimAsH = Math.min(psO.height, psO.width);
                
                //int thisMinDimAsW = Math.min(height, width); 
                int thisMaxDimAsH = Math.min(height, width);
                
                if ( oMaxDimAsH > thisMaxDimAsH){
                    return 1 ; // ? si pour la mêm surface on met le plus grand en hauteur en 1er ?
                }else if (oMaxDimAsH < thisMaxDimAsH){
                    return -1;
                } else {
                    // Alors là on a deux taille de papier de meme surface et de même hauteur ... donc ils doivent etre identique en largeur ...
                    // alors utilison le nom associer 
                    return name.compareToIgnoreCase(psO.name);
                }
                
                }
                // { ici on ne devrait jamais arriver  ... cela voudrais dire qu'il y a un defaut de logique ...}
                //throw new Exception("Coding conceptual error ... ( bad PPAC or bad modification ? )");// ouf il faut commenter cela pour compiler :) 
            } 
            else if (o instanceof PaperSettings) {// pas forcement utile ... mais voudrais en avoir la posibilité.
                //
                ((PaperSettings)o).getPaperWidthFromPanel();
                ((PaperSettings)o).getPaperHeightFromPanel();
                //return this.compareTo(new PaperSize(name, width, height));

            } 
            else if (o instanceof Paper) { // pas forcement utile ... mais je préfére en avoir la posibilité ...
                double pH = ((Paper)o ).getPaperHeight();//la aussi je voudrais plus simple ... mais là je peut y arriver simplemeyy
                double pW = ((Paper)o ).getPaperHeight();
                
            }

        }
        return -1;
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    // Jeu d'essai Comparable ...
    
    public static void main(String[] args) {
        TreeMap<PaperSize,Integer> map = new TreeMap<>( (o1, o2) -> {
            //return 0; // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/LambdaBody
            return o1.name.compareTo(o2.name);
        });
        
        for ( PaperSize ps : PaperSettings.commonPaperSizes){
            map.put(ps, 1);
        }
        
        for ( PaperSize p : map.keySet()){
            System.out.println(String.format("%-20s %5d x %5d mm ( = %7d mm² )", p.name, p.width,p.height, p.width * p.height));
        }
        
        System.out.println("");        
        System.out.println("A set with only one element cause the comparator methode return 0 for all comparaison ... so all is the same and a set by definition dont have twice the same element");        
        SortedSet<PaperSize> otherSort = new TreeSet((o1, o2) -> {
            return 0; // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/LambdaBody
        });
        otherSort.addAll(map.keySet());
        for ( PaperSize p : otherSort){
             System.out.println(String.format("%-20s %5d x %5d mm ( = %7d mm² )", p.name, p.width,p.height, p.width * p.height));
        }
        
        
         System.out.println("");        
        System.out.println("Par surface");        
        TreeSet<PaperSize> otherSortSUrface = new TreeSet((o1, o2) -> {
            return (((PaperSize)o1).width * ((PaperSize)o1).height ) - ( ((PaperSize)o2).width * ((PaperSize)o2).height ); 
        });
        otherSortSUrface.addAll(map.keySet());
        for ( PaperSize p : otherSortSUrface){
             System.out.println(String.format("%-20s %5d x %5d mm ( = %7d mm² )", p.name, p.width,p.height, p.width * p.height));
        }
    }
}