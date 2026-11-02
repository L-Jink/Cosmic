/*
    Quest 1028 - To Lith Harbor!  (chain "For a new world")
    Start NPC: Shanks (22000, Southperry)   End NPC: Olaf (1002101, Lith Harbor)

    Quest.wz/Check.img.xml gives quest 1028 a client "startscript" (q1028s), so the client asks the server for a
    scripted start (QuestActionHandler case 4). Without this file the server logged "START Quest 1028 is uncoded"
    and the quest could never be started (P0nk/Cosmic #324).

    Start requirements (level <= 10, Beginner, quest 1027 completed, item 1042003) are checked by the engine
    before this runs. Completion and rewards (130 exp, 10x 2010001, 10x 2010004) come from Quest.wz/Act, so Olaf
    needs no end script.

    The original q1028s dialog text is not recorded in the WZ data we have, so no text is invented here:
    the quest is just started. The boat ride itself stays in npc/22000.js.
*/
function start(mode, type, selection) {
    qm.forceStartQuest();
    qm.dispose();
}

function end(mode, type, selection) {
    qm.dispose();
}
