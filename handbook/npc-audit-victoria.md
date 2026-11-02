# NPC audit - Victoria Island + Maple Island (card E1-1)

Generated from `wz/Map.wz` life entries for Maple Island maps and 100-105xxxxxx (Henesys, Ellinia, Perion, Kerning, Lith Harbor, Sleepywood towns and their field maps) compared with `scripts/npc`, the shop tables and `Quest.wz/Check`.
Status: ok / fixed / missing / unsure. "ok" for scripts means: file exists and parses; wording was not re-diffed against GMS.

| NPC id | Name | Region | First map | Status | Note |
|---|---|---|---|---|---|
| 10000 | Pio | Maple Island | 001000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1001000 | Silver | Lith Harbor | 104000003 | ok | shop-only NPC (no script needed) |
| 1001001 | Natasha | Lith Harbor | 104000001 | ok | shop-only NPC (no script needed) |
| 1001100 | Mina | Lith Harbor | 104000002 | ok | shop-only NPC (no script needed) |
| 1002000 | Phil | Lith Harbor | 104000000 | ok | script npc/1002000.js (syntax checked; text not re-verified line by line) |
| 1002001 | Teo | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((33 quests,1 scripted)) |
| 1002002 | Pason | Lith Harbor | 104000000 | ok | script npc/1002002.js (syntax checked; text not re-verified line by line) |
| 1002003 | Mr. Goldstein | Lith Harbor | 104000000 | ok | script npc/1002003.js (syntax checked; text not re-verified line by line) |
| 1002004 | VIP Cab | Lith Harbor | 104000000 | ok | script npc/1002004.js (syntax checked; text not re-verified line by line) |
| 1002005 | Mr. Kim | Lith Harbor | 104000000 | ok | script npc/1002005.js (syntax checked; text not re-verified line by line) |
| 1002006 | Chef | Lith Harbor | 104000000 | ok | script npc/1002006.js (syntax checked; text not re-verified line by line) |
| 1002007 | Regular Cab at Lith Harbor | Lith Harbor | 104000000 | ok | script npc/1002007.js (syntax checked; text not re-verified line by line) |
| 1002100 | Jane | Lith Harbor | 104000000 | ok | script npc/1002100.js (syntax checked; text not re-verified line by line) |
| 1002101 | Olaf | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((14 quests,0 scripted)) |
| 1002103 | Captain Al | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((4 quests,1 scripted)) |
| 1002104 | Tru | Lith Harbor | 104000004 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((26 quests,15 scripted)) |
| 1010100 | Rina | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((7 quests,0 scripted)) |
| 1011000 | Karl | Henesys | 100000101 | ok | shop-only NPC (no script needed) |
| 1011001 | Sam | Henesys | 100000101 | ok | shop-only NPC (no script needed) |
| 1011100 | Luna | Henesys | 100000102 | ok | shop-only NPC (no script needed) |
| 1012000 | Regular Cab | Henesys | 100000000 | ok | script npc/1012000.js (syntax checked; text not re-verified line by line) |
| 1012002 | Vicious | Henesys | 100000100 | ok | script npc/1012002.js (syntax checked; text not re-verified line by line) |
| 1012003 | Chief Stan | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((26 quests,0 scripted)) |
| 1012004 | Doofus | Henesys | 100000100 | ok | shop-only NPC (no script needed) |
| 1012005 | Cloy | Henesys | 100000200 | ok | script npc/1012005.js (syntax checked; text not re-verified line by line) |
| 1012006 | Trainer Bartos | Henesys | 100000202 | ok | script npc/1012006.js (syntax checked; text not re-verified line by line) |
| 1012007 | Trainer Frod | Henesys | 100000202 | ok | script npc/1012007.js (syntax checked; text not re-verified line by line) |
| 1012008 | Casey | Henesys | 100000203 | ok | script npc/1012008.js (syntax checked; text not re-verified line by line) |
| 1012009 | Mr. Lee | Henesys | 100000200 | ok | script npc/1012009.js (syntax checked; text not re-verified line by line) |
| 1012100 | Athena Pierce | Henesys | 100000201 | ok | script npc/1012100.js (syntax checked; text not re-verified line by line) |
| 1012101 | Maya | Henesys | 100000001 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((15 quests,0 scripted)) |
| 1012102 | Pia | Henesys | 100000200 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((7 quests,0 scripted)) |
| 1012103 | Natalie | Henesys | 100000104 | ok | script npc/1012103.js (syntax checked; text not re-verified line by line) |
| 1012104 | Brittany | Henesys | 100000104 | ok | script npc/1012104.js (syntax checked; text not re-verified line by line) |
| 1012105 | Ms. Tan | Henesys | 100000105 | ok | script npc/1012105.js (syntax checked; text not re-verified line by line) |
| 1012106 | Mrs. Ming Ming | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((14 quests,0 scripted)) |
| 1012108 | Camila | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((7 quests,0 scripted)) |
| 1012109 | Jay | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((20 quests,0 scripted)) |
| 1012110 | Anne | Henesys | 100010000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((4 quests,0 scripted)) |
| 1012111 | Bruce | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((10 quests,2 scripted)) |
| 1012112 | Tory | Henesys | 100000200 | ok | script npc/1012112.js (syntax checked; text not re-verified line by line) |
| 1012115 | Henesys Forest | Henesys | 100020000 | ok | script npc/1012115.js (syntax checked; text not re-verified line by line) |
| 1012116 | Henesys Forest | Henesys | 100020000 | ok | script npc/1012116.js (syntax checked; text not re-verified line by line) |
| 1012117 | Big Headward | Henesys | 100000104 | ok | script npc/1012117.js (syntax checked; text not re-verified line by line) |
| 1012119 | Power B. Fore | Henesys | 100010000 | ok | script npc/1012119.js (syntax checked; text not re-verified line by line) |
| 10200 | Athena Pierce | Maple Island | 001020000 | ok | script npc/10200.js (syntax checked; text not re-verified line by line) |
| 10201 | Grendel the Really Old | Maple Island | 001020000 | ok | script npc/10201.js (syntax checked; text not re-verified line by line) |
| 10202 | Dances with Balrog | Maple Island | 001020000 | ok | script npc/10202.js (syntax checked; text not re-verified line by line) |
| 10203 | Dark Lord | Maple Island | 001020000 | ok | script npc/10203.js (syntax checked; text not re-verified line by line) |
| 10204 | Kyrin | Maple Island | 001020000 | ok | script npc/10204.js (syntax checked; text not re-verified line by line) |
| 1021000 | River | Perion | 102000001 | ok | shop-only NPC (no script needed) |
| 1021001 | Harry | Perion | 102000001 | ok | shop-only NPC (no script needed) |
| 1021100 | Arturo | Perion | 102000002 | ok | shop-only NPC (no script needed) |
| 1022000 | Dances with Balrog | Perion | 102000003 | ok | script npc/1022000.js (syntax checked; text not re-verified line by line) |
| 1022006 | Winston | Ellinia | 101030401 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((18 quests,0 scripted)) |
| 1022008 | Burnt Sword | Ellinia | 101030402 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1022100 | Sophia | Perion | 102000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((5 quests,0 scripted)) |
| 1022101 | Rooney | Henesys | 100000000 | ok | script npc/1022101.js (syntax checked; text not re-verified line by line) |
| 1022102 | The Excavator Board | Ellinia | 101030104 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1022105 | Power B. Fore | Ellinia | 101040000 | ok | script npc/1022105.js (syntax checked; text not re-verified line by line) |
| 1031000 | Flora the Fairy | Ellinia | 101000001 | ok | shop-only NPC (no script needed) |
| 1031001 | Serabi the Fairy | Ellinia | 101000001 | ok | shop-only NPC (no script needed) |
| 1031100 | Len the Fairy | Ellinia | 101000002 | ok | shop-only NPC (no script needed) |
| 1032001 | Grendel the Really Old | Ellinia | 101000003 | ok | script npc/1032001.js (syntax checked; text not re-verified line by line) |
| 1032004 | Louis | Ellinia | 101000100 | ok | script npc/1032004.js (syntax checked; text not re-verified line by line) |
| 1032007 | Joel | Ellinia | 101000300 | ok | script npc/1032007.js (syntax checked; text not re-verified line by line) |
| 1032008 | Cherry | Ellinia | 101000300 | ok | script npc/1032008.js (syntax checked; text not re-verified line by line) |
| 1032009 | Purin | Ellinia | 101000301 | ok | script npc/1032009.js (syntax checked; text not re-verified line by line) |
| 1032102 | Mar the Fairy | Ellinia | 101000200 | ok | script npc/1032102.js (syntax checked; text not re-verified line by line) |
| 1032103 | El Moth | Ellinia | 101010102 | ok | shop-only NPC (no script needed) |
| 1032105 | Estelle | Henesys | 100050000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((8 quests,0 scripted)) |
| 1032111 | Small Tree Stump | Ellinia | 101010103 | ok | script npc/1032111.js (syntax checked; text not re-verified line by line) |
| 1032112 | Talking Tree | Henesys | 100040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1032114 | Power B. Fore | Henesys | 100050000 | ok | script npc/1032114.js (syntax checked; text not re-verified line by line) |
| 1040002 | Fanzy | Ellinia | 101020000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((4 quests,0 scripted)) |
| 1043000 | a pile of flowers | Ellinia | 101000101 | ok | script npc/1043000.js (syntax checked; text not re-verified line by line) |
| 1043001 | a pile of herbs | Ellinia | 101000104 | ok | script npc/1043001.js (syntax checked; text not re-verified line by line) |
| 1051000 | Cutthroat Manny | Kerning | 103000001 | ok | shop-only NPC (no script needed) |
| 1051001 | Don Hwang | Kerning | 103000001 | ok | shop-only NPC (no script needed) |
| 1051002 | Dr. Faymus | Kerning | 103000002 | ok | shop-only NPC (no script needed) |
| 1052001 | Dark Lord | Kerning | 103000003 | ok | script npc/1052001.js (syntax checked; text not re-verified line by line) |
| 1052003 | Chris | Kerning | 103000006 | ok | script npc/1052003.js (syntax checked; text not re-verified line by line) |
| 1052004 | Denma the Owner | Henesys | 100000103 | ok | script npc/1052004.js (syntax checked; text not re-verified line by line) |
| 1052005 | Dr. Feeble | Henesys | 100000103 | ok | script npc/1052005.js (syntax checked; text not re-verified line by line) |
| 1052006 | Jake | Kerning | 103000100 | ok | script npc/1052006.js (syntax checked; text not re-verified line by line) |
| 1052007 | The Ticket Gate | Kerning | 103000100 | ok | script npc/1052007.js (syntax checked; text not re-verified line by line) |
| 1052008 | Treasure Chest | Kerning | 103000902 | ok | script npc/1052008.js (syntax checked; text not re-verified line by line) |
| 1052009 | Treasure Chest | Kerning | 103000905 | ok | script npc/1052009.js (syntax checked; text not re-verified line by line) |
| 1052010 | Treasure Chest | Kerning | 103000909 | ok | script npc/1052010.js (syntax checked; text not re-verified line by line) |
| 1052011 | Exit | Kerning | 103000900 | ok | script npc/1052011.js (syntax checked; text not re-verified line by line) |
| 1052100 | Don Giovanni | Kerning | 103000005 | ok | script npc/1052100.js (syntax checked; text not re-verified line by line) |
| 1052101 | Andre | Kerning | 103000005 | ok | script npc/1052101.js (syntax checked; text not re-verified line by line) |
| 1052105 | Jane Doe | Kerning | 103000004 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,0 scripted)) |
| 1052107 | Small Street Light | Kerning | 103000105 | ok | script npc/1052107.js (syntax checked; text not re-verified line by line) |
| 1052109 | Subway Trash Can | Kerning | 103000101 | ok | script npc/1052109.js (syntax checked; text not re-verified line by line) |
| 1052110 | Subway Trash Can | Kerning | 103000101 | ok | script npc/1052110.js (syntax checked; text not re-verified line by line) |
| 1052111 | Subway Trash Can | Kerning | 103000101 | ok | script npc/1052111.js (syntax checked; text not re-verified line by line) |
| 1052112 | Subway Trash Can | Kerning | 103000101 | ok | script npc/1052112.js (syntax checked; text not re-verified line by line) |
| 1052114 | Power B. Fore | Kerning | 103010000 | ok | script npc/1052114.js (syntax checked; text not re-verified line by line) |
| 1052116 | Thompson | Kerning | 103040000 | ok | shop-only NPC (no script needed) |
| 1052117 | Blake | Kerning | 103000310 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,0 scripted)) |
| 1052118 | Lockers | Kerning | 103000310 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1052119 | Lala | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((6 quests,0 scripted)) |
| 1052120 | Maestro Rho | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((5 quests,1 scripted)) |
| 1052121 | Lana | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((12 quests,0 scripted)) |
| 1052122 | Tina | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1052123 | Robby Fray | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,0 scripted)) |
| 1052124 | Marshall | Kerning | 103040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1052125 | June | Kerning | 103040400 | ok | script npc/1052125.js (syntax checked; text not re-verified line by line) |
| 1061000 | Chrishrama | Sleepywood | 105040300 | ok | script npc/1061000.js (syntax checked; text not re-verified line by line) |
| 1061001 | 24 Hr Mobile Store | Sleepywood | 105070001 | ok | shop-only NPC (no script needed) |
| 1061002 | Mr. Sweatbottom | Sleepywood | 105040401 | ok | shop-only NPC (no script needed) |
| 1061003 | Mr. Wetbottom | Sleepywood | 105040402 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1061004 | Ronnie | Ellinia | 101020001 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((8 quests,0 scripted)) |
| 1061005 | Sabitrama | Sleepywood | 105040300 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((6 quests,0 scripted)) |
| 1061006 | Mysterious Statue | Sleepywood | 105040300 | ok | script npc/1061006.js (syntax checked; text not re-verified line by line) |
| 1061007 | Crumbling Statue | Sleepywood | 105040310 | ok | script npc/1061007.js (syntax checked; text not re-verified line by line) |
| 1061008 | Mr. Oh | Sleepywood | 105040300 | ok | script npc/1061008.js (syntax checked; text not re-verified line by line) |
| 1061009 | Door of Dimension | Henesys | 100040106 | ok | script npc/1061009.js (syntax checked; text not re-verified line by line) |
| 1061011 | The Rememberer | Sleepywood | 105040300 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((42 quests,1 scripted)) |
| 1061012 | Insignificant Being | Sleepywood | 105090200 | ok | script npc/1061012.js (syntax checked; text not re-verified line by line) |
| 1061013 | Gwin | Sleepywood | 105090200 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((8 quests,0 scripted)) |
| 1061014 | Mu Young | Sleepywood | 105100100 | ok | script npc/1061014.js (syntax checked; text not re-verified line by line) |
| 1061016 | Suspicious Man | Sleepywood | 105100000 | ok | script npc/1061016.js (syntax checked; text not re-verified line by line) |
| 1061017 | Tristan's Spirit | Sleepywood | 105100101 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1061018 | Mu Young | Sleepywood | 105100300 | ok | script npc/1061018.js (syntax checked; text not re-verified line by line) |
| 1061019 | Ilji | Sleepywood | 105040300 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((4 quests,1 scripted)) |
| 1061100 | Hotel Receptionist | Sleepywood | 105040400 | ok | script npc/1061100.js (syntax checked; text not re-verified line by line) |
| 1063000 | a pile of pink flowers | Sleepywood | 105040311 | ok | script npc/1063000.js (syntax checked; text not re-verified line by line) |
| 1063001 | a pile of blue flowers | Sleepywood | 105040313 | ok | script npc/1063001.js (syntax checked; text not re-verified line by line) |
| 1063002 | a pile of white flowers | Sleepywood | 105040316 | ok | script npc/1063002.js (syntax checked; text not re-verified line by line) |
| 1063003 | Wanted : G. Mushroom | Sleepywood | 105030000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1063004 | Wanted : Curse Eye | Sleepywood | 105040200 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1063005 | Wanted : Evil Eye | Sleepywood | 105070100 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1063007 | Wanted : Z. Mushroom | Sleepywood | 105050300 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1063008 | Wanted : H. Mushroom | Sleepywood | 105050200 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 1063009 | Wanted : Jr. Boogie | Sleepywood | 105070400 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1063011 | Cave Wall of Evil Eye | Sleepywood | 105070300 | unsure | Decoration/scenery NPC, no GMS dialog known - UNCONFIRMED |
| 1063012 | Shaman Rock | Sleepywood | 105050200 | ok | script npc/1063012.js (syntax checked; text not re-verified line by line) |
| 1063013 | Shaman Rock | Sleepywood | 105090000 | ok | script npc/1063013.js (syntax checked; text not re-verified line by line) |
| 1063014 | Mysterious Piece of Paper | Sleepywood | 105050000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 1072000 | Warrior Job Instructor | Perion | 102020300 | ok | script npc/1072000.js (syntax checked; text not re-verified line by line) |
| 1072001 | Magician Job Instructor | Ellinia | 101020000 | ok | script npc/1072001.js (syntax checked; text not re-verified line by line) |
| 1072003 | Thief Job Instructor | Perion | 102040000 | ok | script npc/1072003.js (syntax checked; text not re-verified line by line) |
| 11000 | Sid | Maple Island | 001000001 | fixed | Sid shop: GM shop (1337) no longer shares his npcid (changeset 20261103-0130) |
| 1100007 | Kiriru | Ellinia | 101000400 | ok | script npc/1100007.js (syntax checked; text not re-verified line by line) |
| 1103001 | Roca | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((5 quests,0 scripted)) |
| 11100 | Lucy | Maple Island | 001000003 | ok | shop-only NPC (no script needed) |
| 12000 | Lucas | Maple Island | 001000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((7 quests,0 scripted)) |
| 1200004 | Puro | Lith Harbor | 104000000 | ok | script npc/1200004.js (syntax checked; text not re-verified line by line) |
| 1204033 | John's Box | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,1 scripted)) |
| 12100 | Mai | Maple Island | 001010000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((9 quests,0 scripted)) |
| 12101 | Rain | Maple Island | 001000000 | ok | script npc/12101.js (syntax checked; text not re-verified line by line) |
| 2000 | Roger | Maple Island | 000000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,1 scripted)) |
| 20000 | John | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((6 quests,1 scripted)) |
| 20001 | ? | Maple Island | 000050001 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 20002 | Biggs | Maple Island | 000060000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,0 scripted)) |
| 2001 | Sen | Maple Island | 000000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((5 quests,0 scripted)) |
| 2002 | Peter | Maple Island | 000040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 2003 | Robin | Maple Island | 000050000 | ok | script npc/2003.js (syntax checked; text not re-verified line by line) |
| 2004 | Todd | Maple Island | 000040000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 2005 | Sam | Maple Island | 000050000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((4 quests,0 scripted)) |
| 2006 | Tienk | Lith Harbor | 104000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,1 scripted)) |
| 2007 | Maple Administrator | Maple Island | 000010000 | ok | script npc/2007.js (syntax checked; text not re-verified line by line) |
| 20100 | Yoona | Maple Island | 001010000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((8 quests,0 scripted)) |
| 2040050 | Eurek the Alchemist | Sleepywood | 105040300 | ok | script npc/2040050.js (syntax checked; text not re-verified line by line) |
| 2041017 | Ace of Hearts | Henesys | 100000000 | ok | script npc/2041017.js (syntax checked; text not re-verified line by line) |
| 2100 | Sera | Maple Island | 000000001 | ok | script npc/2100.js (syntax checked; text not re-verified line by line) |
| 21000 | Pan | Maple Island | 000060001 | ok | shop-only NPC (no script needed) |
| 2101 | Heena | Maple Island | 000000002 | ok | script npc/2101.js (syntax checked; text not re-verified line by line) |
| 2102 | Nina | Maple Island | 000000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((5 quests,0 scripted)) |
| 2103 | Maria | Maple Island | 001000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((7 quests,0 scripted)) |
| 22000 | Shanks | Maple Island | 000060000 | ok | x |
| 9000000 | Paul | Maple Island | 000060000 | ok | script npc/9000000.js (syntax checked; text not re-verified line by line) |
| 9000001 | Jean | Lith Harbor | 104000000 | ok | script npc/9000001.js (syntax checked; text not re-verified line by line) |
| 9000007 | Chun Ji | Kerning | 103010000 | ok | script npc/9000007.js (syntax checked; text not re-verified line by line) |
| 9000009 | Vikin | Lith Harbor | 104000000 | ok | script npc/9000009.js (syntax checked; text not re-verified line by line) |
| 9000017 | Coco | Henesys | 100000000 | ok | script npc/9000017.js (syntax checked; text not re-verified line by line) |
| 9000020 | Spinel | Henesys | 100000000 | ok | script npc/9000020.js (syntax checked; text not re-verified line by line) |
| 9000021 | Gaga | Henesys | 100000000 | ok | script npc/9000021.js (syntax checked; text not re-verified line by line) |
| 9000033 | Agent C | Kerning | 103000805 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 9000034 | Agent O | Henesys | 100020000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 9000036 | Agent E | Henesys | 100000000 | ok | script npc/9000036.js (syntax checked; text not re-verified line by line) |
| 9000040 | Dalair | Henesys | 100000200 | ok | script npc/9000040.js (syntax checked; text not re-verified line by line) |
| 9000041 | Donation Box | Henesys | 100000200 | ok | script npc/9000041.js (syntax checked; text not re-verified line by line) |
| 9000068 | Ice Piece | Ellinia | 101010000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 9000069 | Inkwell | Henesys | 100000000 | ok | shop-only NPC (no script needed) |
| 9010000 | Maple Administrator | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((177 quests,0 scripted)) |
| 9010002 | Mia | Henesys | 100000000 | ok | script npc/9010002.js (syntax checked; text not re-verified line by line) |
| 9010009 | Duey | Henesys | 100000000 | ok | Duey: handled in Java (NPCTalkHandler) |
| 9010010 | Cassandra | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((253 quests,0 scripted)) |
| 9010014 | Aramia | Henesys | 100000200 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 9010021 | Wolf Spirit Ryko | Ellinia | 101000005 | ok | script npc/9010021.js (syntax checked; text not re-verified line by line) |
| 9010022 | Dimensional Mirror | Henesys | 100000000 | ok | script npc/9010022.js (syntax checked; text not re-verified line by line) |
| 9020001 | Cloto | Kerning | 103000800 | ok | script npc/9020001.js (syntax checked; text not re-verified line by line) |
| 9020002 | Nella | Kerning | 103000800 | ok | script npc/9020002.js (syntax checked; text not re-verified line by line) |
| 9040000 | Shuang | Ellinia | 101030104 | ok | script npc/9040000.js (syntax checked; text not re-verified line by line) |
| 9040002 | Shawn | Ellinia | 101030104 | ok | script npc/9040002.js (syntax checked; text not re-verified line by line) |
| 9040008 | Guild Rank Board | Ellinia | 101030104 | ok | script npc/9040008.js (syntax checked; text not re-verified line by line) |
| 9040011 | Bulletin Board | Ellinia | 101030104 | ok | script npc/9040011.js (syntax checked; text not re-verified line by line) |
| 9100100 | Gachapon | Henesys | 100000100 | ok | Gachapon: handled by shared gachapon script (NPCTalkHandler) |
| 9100104 | Gachapon | Sleepywood | 105040300 | ok | Gachapon: handled by shared gachapon script (NPCTalkHandler) |
| 9101001 | Peter | Maple Island | 000000003 | ok | script npc/9101001.js (syntax checked; text not re-verified line by line) |
| 9101002 | Todd | Maple Island | 000000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 9101003 | Peter | Maple Island | 000000002 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 9102100 | ? | Henesys | 100000202 | ok | script npc/9102100.js (syntax checked; text not re-verified line by line) |
| 9102101 | ? | Henesys | 100000202 | ok | script npc/9102101.js (syntax checked; text not re-verified line by line) |
| 9200000 | Cody | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((21 quests,0 scripted)) |
| 9200100 | Dr. Lenu | Henesys | 100000103 | ok | script npc/9200100.js (syntax checked; text not re-verified line by line) |
| 9201001 | Nana(H) | Henesys | 100000000 | ok | script npc/9201001.js (syntax checked; text not re-verified line by line) |
| 9201022 | Thomas Swift | Henesys | 100000000 | ok | script npc/9201022.js (syntax checked; text not re-verified line by line) |
| 9201057 | Bell | Kerning | 103000100 | ok | script npc/9201057.js (syntax checked; text not re-verified line by line) |
| 9201088 | Barry | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((10 quests,0 scripted)) |
| 9201118 | Hill | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((10 quests,0 scripted)) |
| 9201122 | Old Bow | Ellinia | 101010102 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((2 quests,0 scripted)) |
| 9201123 | Warrior Statue | Lith Harbor | 104000000 | ok | script npc/9201123.js (syntax checked; text not re-verified line by line) |
| 9201124 | Bowman Statue | Lith Harbor | 104000000 | ok | script npc/9201124.js (syntax checked; text not re-verified line by line) |
| 9201125 | Magician Statue | Lith Harbor | 104000000 | ok | script npc/9201125.js (syntax checked; text not re-verified line by line) |
| 9201126 | Thief Statue | Lith Harbor | 104000000 | ok | script npc/9201126.js (syntax checked; text not re-verified line by line) |
| 9201127 | Pirate Statue | Lith Harbor | 104000000 | ok | script npc/9201127.js (syntax checked; text not re-verified line by line) |
| 9201128 | Demon's Doorway | Perion | 102020300 | ok | script npc/9201128.js (syntax checked; text not re-verified line by line) |
| 9201129 | Demon's Doorway | Ellinia | 101010102 | ok | script npc/9201129.js (syntax checked; text not re-verified line by line) |
| 9201130 | Demon's Doorway | Kerning | 103030200 | ok | script npc/9201130.js (syntax checked; text not re-verified line by line) |
| 9201131 | Demon's Doorway | Lith Harbor | 104040002 | ok | script npc/9201131.js (syntax checked; text not re-verified line by line) |
| 9201132 | Demon's Doorway | Henesys | 100030000 | ok | script npc/9201132.js (syntax checked; text not re-verified line by line) |
| 9201133 | Astaroth's Doorway | Sleepywood | 105050400 | ok | script npc/9201133.js (syntax checked; text not re-verified line by line) |
| 9209000 | Abdula | Henesys | 100000000 | ok | script npc/9209000.js (syntax checked; text not re-verified line by line) |
| 9209001 | Mimi | Henesys | 100000000 | ok | script npc/9209001.js (syntax checked; text not re-verified line by line) |
| 9209008 | Lazy Daisy | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((1 quests,0 scripted)) |
| 9250042 | Henesys Maple TV | Henesys | 100000000 | ok | Maple TV: handled by shared mapleTV script |
| 9250052 | News Stand | Henesys | 100000000 | ok | quest NPC; dialog is driven by Quest.wz + scripts/quest ((3 quests,0 scripted)) |
| 9270001 | Lith Harbor Maple TV | Lith Harbor | 104000000 | ok | Maple TV: handled by shared mapleTV script |
| 9270002 | Sleepywood Maple TV | Sleepywood | 105040300 | ok | Maple TV: handled by shared mapleTV script |
| 9330021 | 니키 | Kerning | 103000007 | unsure | Korean-only (KMS) NPCs placed in 103000007 - not GMS content, UNCONFIRMED |
| 9330022 | 줄리 | Kerning | 103000007 | unsure | Korean-only (KMS) NPCs placed in 103000007 - not GMS content, UNCONFIRMED |
| 9330023 | 허치희 | Kerning | 103000007 | unsure | Korean-only (KMS) NPCs placed in 103000007 - not GMS content, UNCONFIRMED |
| 9330024 | 곽이랑 | Kerning | 103000007 | unsure | Korean-only (KMS) NPCs placed in 103000007 - not GMS content, UNCONFIRMED |
| 9330025 | 소부현 | Kerning | 103000007 | unsure | Korean-only (KMS) NPCs placed in 103000007 - not GMS content, UNCONFIRMED |

Totals: fixed=1, ok=231, unsure=6

Not listed: 9901700-9901739 (40 unnamed NPCs in the Hall of job-instructor maps, event/placeholder ids with no String.wz name).

## Other fixes
- Quest 1028 "To Lith Harbor!" (Shanks -> Olaf): added scripts/quest/1028.js (issue #324). Dialog text UNCONFIRMED (q1028s is client-side; none invented).
- Shop 1337 (!gmshop) shared npcid 11000 with Sid; unlinked by changeset.

## Findings not fixed
- Issue #267 (sendSimple + next disconnect): no script-side cause confirmed in this region; 31 sendSimple calls build their text from variables so could not be statically proven - UNCONFIRMED, likely client/engine.
- Shops 9999992-9999999 all share npcid 9001002 (rank shops) - outside region, same cache-collision pattern; needs a look by cosmic-core/content.
- Shop prices cannot be verified from WZ (WZ price is the sell-back price); prices UNCONFIRMED vs GMS.
- 9901700-39, 9330021-25: non-GMS placeholder/KMS NPCs.
