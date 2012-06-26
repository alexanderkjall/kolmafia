/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.objectpool;

public class IntegerPool
{
	private static final int MIN_VALUE = -2;
	private static final int MAX_VALUE = 12000;

	private static final int RANGE = ( IntegerPool.MAX_VALUE - IntegerPool.MIN_VALUE ) + 1;

	private static final Integer[] CACHE = new Integer[ IntegerPool.RANGE ];

	private static int cacheHits = 0;
	private static int cacheMissHighs = 0;
	private static int cacheMissLows = 0;

	static
	{
		for ( int i = 0; i < IntegerPool.RANGE; ++i )
		{
			IntegerPool.CACHE[ i ] = IntegerPool.MIN_VALUE + i;
		}
	}

	public static int getCacheHits()
	{
		return IntegerPool.cacheHits;
	}

	public static int getCacheMissLows()
	{
		return IntegerPool.cacheMissLows;
	}

	public static int getCacheMissHighs()
	{
		return IntegerPool.cacheMissHighs;
	}

	public static Integer get( int i )
	{
		if ( i < IntegerPool.MIN_VALUE )
		{
			++cacheMissLows;
			return i;
		}

		if ( i > IntegerPool.MAX_VALUE )
		{
			++cacheMissHighs;
			return i;
		}

		++cacheHits;
		return IntegerPool.CACHE[ i - IntegerPool.MIN_VALUE ];
	}
}
