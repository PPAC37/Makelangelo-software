# Study case for : Remember last path from load and save #464
https://github.com/MarginallyClever/Makelangelo-software/issues/464

On vas commencer par regarder commenc c'est fait dans Paper.java ( car cela semble bien fonctionner)

```
	public void loadConfig() {
		Preferences paperPreferenceNode = 
		PreferencesHelper.getPreferenceNode(PreferencesHelper.MakelangeloPreferenceKey.PAPER);
		paperLeft = Double.parseDouble(paperPreferenceNode.get("paper_left", Double.toString(paperLeft)));
		//...
	}
	
	public void saveConfig() {
		Preferences paperPreferenceNode = 
		PreferencesHelper.getPreferenceNode(PreferencesHelper.MakelangeloPreferenceKey.PAPER);
		paperPreferenceNode.putDouble("paper_left", paperLeft);
		//...
	}
```

## 

En gros je passe par où ?
Je reste sur une "node" existante ou j'en créer une pour mon besoin.

Où sont finalement stoqué les préférences ? et quelle est la "node" ?

* ~/makelangelo.xml
* ~/.java/.userPrefs/

```
~/.java/.userPrefs$ find -type f
```

```
./.userRootModFile.q6
./DrawBot/prefs.xml
./DrawBot/File/prefs.xml
./DrawBot/Sound/prefs.xml
./DrawBot/Graphics/prefs.xml
./DrawBot/Language/prefs.xml
./DrawBot/Paper/prefs.xml
./DrawBot/Metrics/prefs.xml
./DrawBot/Machines/prefs.xml
./DrawBot/Machines/0/prefs.xml
./DrawBot/Machines/0/Pen/prefs.xml
./DrawBot/Machines/1/prefs.xml
./.user.lock.q6
./Machines/prefs.xml
./Machines/22453/prefs.xml
./Machines/22453/Pen/prefs.xml
./Machines/0/prefs.xml
./Machines/0/Pen/prefs.xml

```

