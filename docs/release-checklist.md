# Yayın Öncesi Checklist

## A) Kod ve Uygulama Kontrolü

- [ ] Debug ve release derlemeleri başarılı
- [ ] Uygulama açılış crash testi (en az 2 cihaz)
- [ ] Buton/metin taşma kontrolü (küçük ekran + büyük font)
- [ ] Açık/koyu tema kontrolü
- [ ] Türkçe metinler yazım kontrolü
- [ ] Grafiklerde boş veri metni Türkçe görünüyor

## B) Reklam ve Gelir Modeli

- [ ] AdMob App ID dogru
- [ ] Banner Ad Unit ID dogru
- [ ] Debug'da test reklam görünüyor
- [ ] Release'de gerçek reklam birimi kullanılıyor
- [ ] "Reklamları Kaldır" ürünü: `remove_ads_forever`
- [ ] Satın alma sonrası reklam gizleniyor

## C) Politika ve Hukuki Alanlar

- [ ] Gizlilik politikası yayında (public URL)
- [ ] Uygulamada tıbbi sorumluluk notu mevcut
- [ ] Google Play Data Safety formu dolduruldu
- [ ] Reklam beyanları Play Console'da doğru seçildi

## D) Play Store İçeriği

- [ ] Uygulama adı: `Diyabet Takip Asistanı`
- [ ] Kısa açıklama eklendi
- [ ] Uzun açıklama eklendi
- [ ] Uygulama ikonu (512x512)
- [ ] Feature graphic (1024x500)
- [ ] En az 2 ekran görüntüsü

## E) Yayın Akışı

- [ ] Internal testing oluşturuldu
- [ ] AAB yüklendi
- [ ] Test kullanıcısı davet edildi
- [ ] Reklam ve satın alma gerçek test yapıldı
- [ ] Üretim (production) yayınına geçiş kararı verildi

## Not

Internal testte her şey yolundaysa production'a almadan önce bir kez daha:
- [ ] uygulama açılış hızı
- [ ] veri kaybı olmadan güncelleme
- [ ] alarm tetikleme davranışı
kontrol edilmeli.
