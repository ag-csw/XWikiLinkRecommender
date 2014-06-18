Test, dass content in velocity macros nicht angefasst wird:
{{velocity wiki="true" html="false"}}
#set($Zwiebel = "pflanzliches Gewürz")
Eine Zwiebel ist $zwiebel
{{/velocity}}
{{velocity}}
Kerbel
{{/velocity}}