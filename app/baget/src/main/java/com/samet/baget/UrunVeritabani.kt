// UrunVeritabani.kt
package com.samet.baget

class UrunVeritabani {
    private val urunler = mutableMapOf<String, Urun>()

    fun urunEkle(barkod: String, ad: String, kgFiyati: Float) {
        urunler[barkod] = Urun(ad, kgFiyati)
    }

    fun urunBul(barkod: String): Urun? {
        return urunler[barkod]
    }
}

data class Urun(val ad: String, val kgFiyati: Float)