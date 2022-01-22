# 

https://github.com/jpomykala/awesome-i18n


Traduction in https://github.com/MarginallyClever/Makelangelo-software
Makelangelo version 2.28... ( may be subject to change for specifi version )

Pour les modifier il suffit en principe d'en telecharger un et le le placer dans le même repertoire que le .jar.
(Normalement les fichier de traduction chargé au démmarge sont visible dans le logs si vous definisé avant de lancer vorte jar DEV=true ou en passant avant le -jar l'argument -Denv=true )
(Normalement si un fichier de traduction a correctement etait chargé on dois retrouver dans les préférences, languages, le langage qu'il définie dans le JComboBox ... ( except ... overwrite an existaing )
Pour qu'une modification soit prise en compte il est préférable de relancer l'application ( fichier de traduction chargé uniquement au démmarage)

voir ISO 639 langue

voir ISO 3166 Pays

! selon le pays / region une même langue peut avoir des variantes
9 = (en_GB) Nine 
9 = (fr_FR) Neuf = (fr_BE) Nonante

(fr_FR) Voiture = (fr_CA) Char

...

enore plus délicat quand la sencure et/ou les mentalité et/ou les evolutions sociétal 
rentre en jeu ( "Tintin au congo" réédition exemple )mais dans le contexte de makelangelo on ne devrait pas avoir de problèmes.




##
les fichiers de traductions sont normalement dans
https://github.com/MarginallyClever/Makelangelo-software/tree/master/src/main/resources/languages
(ils sont lié a une version, car des clés et valeur de traduction peuvent changer (ajout, supression, modification de la clé et de la valeur de traduction)

### (en) english.xml // English ?= en_GB
https://github.com/MarginallyClever/Makelangelo-software/blob/master/src/main/resources/languages/english.xml
https://github.com/MarginallyClever/Makelangelo-software/raw/master/src/main/resources/languages/english.xml
### (nl) dutch.xml // Nederlands ?= nl_NL

###  chinese.xml // Chinese ?= zh-CN

### de german.xml // Deutsch ?= de_AL

### ?? piglatin.xml // Pig Latin ?= ??

https://github.com/MarginallyClever/Makelangelo-software/wiki#translators

https://www.marginallyclever.com/2020/06/how-to-translate-makelangelo-software/

## language xml files validation

### xml well formed
xml well formed ( check if the xml is weel formed, respect the basic xml grammare. like an open tag have a close tag , a tage name do not containe spécial chars ...

Pour vérifier que le contenu xml dans le fichier est bien formé. (respecte les recommandations w3c sur le format xml)
* un element / une balise ouvrant a bien un une balise fermante ou est un element auto fermant.
* un nom de balise respecte les recommandations
*  ...

#### xml reserved chars
As in xml '&', '<' and '>' are reserved charactéres in some place you have to converte/transliterat  them.
, excepte if used in a CDATA bloc 
```
<![CDATA[...Me texte...]]>
```
 , you have to transliterat? it.

'&' -> "&amp;"
'<' -> "&lt;"
'>' -> "&gt;"
...

```

	<string>
		<key>LissajousB</key>
		<value>B (&gt;0)</value>
		<hint>Lissajous generator</hint>
	</string>

```
...
```
<string>
		<key>ConverterCMYKNote</key>
		<value><![CDATA[<html>Draws Yellow, Cyan, Magenta, and then Black.</html></body>]]></value>
		<hint>CMYK converter hint</hint>
	</string>

```

#### Char limitation

In specific usage / in some contexte / in some place
other limitation cane be enforced
...

### DTD and XSD for upper Validation

xml validation ( check if the xml validate a grammar definition (.dtd ou .xsl) , all tag name used are know, have or not specific elements ...)

Pour valider le contenu du fichier .xml en vérifiant ... le langage/vocabulaier et la syntaxe.

* les nom de balise utilisé sont ils bien definie dans le langage) 

Je vous propose la dtd suivante pour valider le fichier.


je vous propose la ... suivante


https://www.freeformatter.com/xsd-generator.html#ad-output

xsd seem to be a better way ... (more controles, ...)
dtd can be use to generate parser / scanner ... (under netbeans ... SAX)
# 

### VRAC A TRIER


? max char count ( ssi limitation / trunc )
! blanc space at the end can be needed 

Length limitation depande of the JComponent / Object that use the texte
Keep it short / simple the more you can

Normaly a JLabel cant interpret '\n' line retrurn ( but in some case a <br/> can be use ...)


Line limitation

? activer le mode bebug/trace des traductions ? ( CLI et/ou pref ?  )

? idiome de la langue
Expression qui n'a pas déquivlance en traduction mot a mot dans un autre langage mais qui peut avoir une expresion equivalente
(fr) "QUand les poule aurons de dents "
(en) "when pig can fly"

? no traduction
en hinuite il existe un trés vaste vocabulaire pour décrire la neige et peut ou aucune autre langue n'a déquivalenc epour chaque mot.

? sens des mots ( dans une langue un mot peut avoir plusieur signification est une traduction peut perdre ou ne plus avoir la mêm signification (lié a une utilisation dans un contexte dans pour une signification particuliére )


### Makelangelo implementation

#### Limitation due to implementation 

##### if duplicated elements only the last value in the xml file is used.
https://github.com/MarginallyClever/Makelangelo-software/blob/03e9f11afac3e7badf82b8851d435d81fc305c14/src/main/java/com/marginallyclever/makelangelo/TranslatorLanguage.java#L133


https://github.com/MarginallyClever/Makelangelo-software/blob/03e9f11afac3e7badf82b8851d435d81fc305c14/src/main/resources/languages/german.xml#L2

https://github.com/MarginallyClever/Makelangelo-software/blob/03e9f11afac3e7badf82b8851d435d81fc305c14/src/main/resources/languages/german.xml#L1061-L1062

### DTD proposition

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!DOCTYPE language
[
<!-- a DTD definition so we can xml validate the file 
-->
<!ELEMENT language (meta,string+)>
<!ATTLIST language   xml:lang NMTOKEN 'en'> <!-- to allow an attribut lang define with the value 'en' as default -->

<!ELEMENT meta (name,author)>
<!ELEMENT name (#PCDATA)>
<!ELEMENT author (#PCDATA)>

<!ELEMENT string (key,value,hint?)> <!-- remove the ? at the and of "hint?" to have validation issue for the <string> that do not containe a <hint>  -->

<!ELEMENT key (#PCDATA)> <!-- 
// TODO to reveiw for key unicity, migrate the elements as and attribut id of the <strnig> elements
<!ELEMENT string (value,hint?)>
<!ATTLIST string id ID #IMPLIED>
// but have to change the read/write implementation as an element becom an attribut .
TODO study case code change / xml rework / implication (no back compatibility )

  -->				
<!ELEMENT value (#PCDATA)>
<!ELEMENT hint (#PCDATA)>


]>

<language xml:lang="en" >
	<meta>
		<name>English</name>
		<author>Dan Royer (dan@marginallyclever.com)</author>
	</meta>

	<string>
		<key>BorderName</key>
		<value>Outline paper</value>
		<hint>Border generator</hint>
	</string>
...
```

https://www.lehtml.com/xml/dtd.html

https://www.lehtml.com/xml/xml10.html#syntax

https://www.lehtml.com/xml/xml10.html#NT-Langcode


https://www.lehtml.com/xml/xml10.html#sec-cdata-sect



? empecher que key et name ne soit vide
? key doit etre unique 



https://www.irif.fr/~carton/Enseignement/XML/Cours/DTD/index.html#sect.dtd.attribute.id 

``̀`
<!ATTLIST string id ID #IMPLIED>
`̀ `
can become to force to have an id attribut
``̀`
<!ATTLIST string id ID #REQUIRED>
`̀ `


// mais cela implique une limitation des valeur utilisabel pour les ID cf en xml les "NCName"
https://www.irif.fr/~carton/Enseignement/XML/Cours/Syntax/index.html#sect.syntax.name


