/*
 * Copyright (C) 2022 Marginally Clever Robots, Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.marginallyclever.makelangelo.makeArt.io.vector;

/**
 *
 * @author q6
 */
public class MathToRadian {
	public static void main(String[] args) {
		double a=10;
		for ( double angInDeg = -360 ; angInDeg <=360 ; angInDeg = angInDeg+a){
			System.out.printf("°=%5f \tR=%5f \t°=%5f\n",angInDeg,Math.toRadians(angInDeg),Math.toDegrees(Math.toRadians(angInDeg)));//,Math.toDegrees(Math.cos(Math.toRadians(v-90)))
		}
	}
}
