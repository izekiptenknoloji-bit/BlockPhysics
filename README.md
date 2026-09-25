# BlockPhysics

Paper 1.21.x icin, Minecraft'ta olmayan **gercekci blok dusme/kayma fizigi** ekleyen eklenti.

## Ne yapar?

Vanilla Minecraft'ta sadece kum/cakil gibi birkac blok yercekimine tabidir ve destegi
kalktiginda direkt asagi duser, dumduz zemine oturur. Bu eklenti ile:

- `config.yml` icinde belirtilen blok turleri (varsayilan: tas, toprak, odun, beton vb.
  "basit" bloklar) destegini kaybettiginde (altindaki blok kirilinca, patlayinca, yaninca
  vb.) gercek bir `FallingBlock` varligina donusur ve duser.
- Duz olmayan bir yuzeye (kenar/ucurum) inerse, bos tarafa dogru kayar/yuvarlanir ve
  tekrar duser; boylece merdiven/ucurum kenarlarinda birikme yerine gercekci bir sekilde
  asagi akar.
- Zincirleme cokme: bir blok kaldiginda ustundeki blok da destegini kaybediyorsa o da
  tetiklenir (performans icin `max-cascade-per-event` ile sinirlanir).
- Yerlesecek uygun yer yoksa (baska bir bloga carparsa) item olarak duser.

**Not:** Varsayilan liste, envanter tutan bloklari (sandik, firin vb.) veya coklu-blok
yapilari (kapi, yatak) icermez — bu bloklarin `FallingBlock`'a donusmesi veri kaybina
yol acabilir. Kum/cakil/beton tozu gibi vanilla'da zaten yercekimine tabi bloklari da
listeye EKLEMEYIN, cift tetiklenme (duplicate) riskine girer.

## Derleme

Sistemde Maven kurulu olmasa bile proje kendi Maven Wrapper'ini iceriyor:

```powershell
.\mvnw.cmd clean package
```

Derlenen eklenti `target/BlockPhysics-1.0.0.jar` olarak olusur. Bu dosyayi sunucunun
`plugins/` klasorune kopyalayip sunucuyu (yeniden) baslatmaniz yeterli.

## Komutlar / Izinler

- `/blockphysics reload|toggle|status` (kisaltmalar: `/bphysics`, `/bp`)
- Izin: `blockphysics.admin` (varsayilan: op)

## Ayarlar (`config.yml`)

- `enabled`: eklentiyi tamamen ac/kapa
- `blocks.mode`: `whitelist` (sadece listedekiler) veya `blacklist` (listedekiler haric
  tum solid bloklar — dikkatli kullanin, performans/oyun dengesini etkileyebilir)
- `blocks.list`: etkilenecek/etkilenmeyecek blok turleri
- `physics.max-slide-attempts`: bir blogun inince kac kez kenardan kaymayi deneyecegi
- `physics.max-cascade-per-event`: tek olayda tetiklenebilecek maksimum zincirleme blok
- `physics.drop-item-if-no-space`: yerlesecek yer yoksa item olarak dussun mu
- `effects.sound` / `effects.particles`: yerlesme efektleri

## Bilinen sinirlamalar (sonraki adimlar icin)

- Destek kontrolu sadece "altindaki blok solid mi" kuralina dayanir (duvar gibi yanal
  destek hesaplanmiyor). Gercek yapisal butunluk analizi kapsam disi birakildi.
- Piston itme/cekme olaylari henuz tetikleyici olarak eklenmedi.
- Gorsel "yuvarlanma" rotasyonu yoktur; fizik yalnizca dusme + kenardan kayma yoluyla
  hissettirilir (FallingBlock varliklari donme animasyonu desteklemez).
