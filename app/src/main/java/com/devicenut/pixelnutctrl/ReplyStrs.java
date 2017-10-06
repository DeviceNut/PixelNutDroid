package com.devicenut.pixelnutctrl;

import android.util.Log;

import static com.devicenut.pixelnutctrl.Main.CMD_GET_PATTERNS;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_PLUGINS;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_SEGMENTS;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_FORCE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.MINLEN_SEGLEN_FORADV;
import static com.devicenut.pixelnutctrl.Main.MINLEN_CMDSTR_PERSEG;
import static com.devicenut.pixelnutctrl.Main.MINVAL_DELAYRANGE;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.advPatternBits;
import static com.devicenut.pixelnutctrl.Main.advPatternCmds;
import static com.devicenut.pixelnutctrl.Main.advPatternHelp;
import static com.devicenut.pixelnutctrl.Main.advPatternNames;
import static com.devicenut.pixelnutctrl.Main.advPatternsCount;
import static com.devicenut.pixelnutctrl.Main.basicPatternBits;
import static com.devicenut.pixelnutctrl.Main.basicPatternCmds;
import static com.devicenut.pixelnutctrl.Main.basicPatternHelp;
import static com.devicenut.pixelnutctrl.Main.basicPatternNames;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.customPlugins;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.editPatterns;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;
import static com.devicenut.pixelnutctrl.Main.maxlenCmdStrs;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.segPosCount;
import static com.devicenut.pixelnutctrl.Main.segPosStart;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;
import static com.devicenut.pixelnutctrl.Main.segLayers;
import static com.devicenut.pixelnutctrl.Main.segPixels;
import static com.devicenut.pixelnutctrl.Main.segTracks;

class ReplyStrs
{
    private final String LOGNAME = "ReplyStrs";

    private int replyState;
    private int optionLines;
    private boolean replyFail;
    private boolean didFinishReading;

    private boolean setPercentage;
    private boolean getSegments;
    private boolean getPatterns;
    private boolean getPlugins;

    double progressPercent;
    double progressPcentInc;
    String sendCmdStr;

    ReplyStrs()
    {
        replyState = 0;
        replyFail = false;
        setPercentage = false;
        didFinishReading = false;
    }

    private void CheckSegVals(int i)
    {
        if (!CheckValue(segPatterns[i],  0, numPatterns-1))  segPatterns[i] = 0;
        if (!CheckValue(segXmodeHue[i],  0, MAXVAL_HUE))     segXmodeHue[i] = 0;
        if (!CheckValue(segXmodeWht[i],  0, MAXVAL_PERCENT)) segXmodeWht[i] = 0;
        if (!CheckValue(segXmodeCnt[i],  0, MAXVAL_PERCENT)) segXmodeCnt[i] = 0;
        if (!CheckValue(segTrigForce[i], 0, MAXVAL_FORCE))   segTrigForce[i] = MAXVAL_FORCE >> 1;
    }

    private void CheckForExtendedCommands()
    {
        getSegments = (numSegments > 1);
        getPatterns = (customPatterns > 0);
        getPlugins = (customPlugins > 0);
        setPercentage = (getSegments || getPatterns || getPlugins);
    }

    int Next(String reply)
    {
        Log.v(LOGNAME, "ReplyState=" + replyState + " OptionLines=" + optionLines);

        if (replyFail || didFinishReading)
        {
            Log.e(LOGNAME, "Reply after finish: " + reply);
            replyFail = true;
            return 0;
        }
        else if ((replyState > 1) && (optionLines <= 0))
        {
            Log.w(LOGNAME, "Unexpected reply: " + reply);
            replyFail = true;
        }
        else if (getSegments)
        {
            if (replyState == 1)
            {
                if (multiStrands)
                {
                    String[] strs = reply.split("\\s+"); // remove ALL spaces
                    for (int i = 0, j = 0; (i + 1) < strs.length; ++j)
                    {
                        if (i >= (3 * segPixels.length)) break; // prevent overrun

                        int val1 = Integer.parseInt(strs[i++]);
                        int val2 = Integer.parseInt(strs[i++]);
                        int val3 = Integer.parseInt(strs[i++]);
                        Log.v(LOGNAME, ">> Segment Info " + j + ": " + val1 + ":" + val2 + ":" + val3);

                        segPixels[j] = val1;
                        segLayers[j] = val2;
                        segTracks[j] = val3;

                        if (!CheckValue(segPixels[j], 1, 0) ||
                            !CheckValue(segLayers[j], 2, 0) ||
                            !CheckValue(segTracks[j], 1, 0))
                            replyFail = true;

                        if (strs.length == 3) // only first set of values has been sent
                        {
                            while (++j < numSegments)
                            {
                                segPixels[j] = val1;
                                segLayers[j] = val2;
                                segTracks[j] = val3;
                            }
                            break;
                        }
                    }
                }
                else
                {
                    String[] strs = reply.split("\\s+"); // remove ALL spaces
                    for (int i = 0, j = 0; (i + 1) < strs.length; ++j)
                    {
                        if (i >= (2 * segPosStart.length)) break; // prevent overrun

                        int val1 = Integer.parseInt(strs[i++]);
                        int val2 = Integer.parseInt(strs[i++]);
                        Log.v(LOGNAME, ">> Segment Extents " + j + ": " + val1 + ":" + val2);

                        segPosStart[j] = val1;
                        segPosCount[j] = val2;

                        // if any segment is very short then just use basic patterns
                        if (val2 < MINLEN_SEGLEN_FORADV) useAdvPatterns = false;
                    }
                }
            }
            else if (replyState <= numSegments+1)
            {
                int segindex = replyState-2;
                Log.v(LOGNAME, "SegValues[" + segindex + "]: " + reply);

                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 8)
                {
                    curBright[    segindex] = Integer.parseInt(strs[0]);
                    curDelay[     segindex] = (byte)Integer.parseInt(strs[1]);
                    segPatterns[  segindex] = Integer.parseInt(strs[2]);
                    segXmodeEnb[  segindex] = strs[3].charAt(0) != '0';
                    segXmodeHue[  segindex] = Integer.parseInt(strs[4]) + (Integer.parseInt(strs[5]) << 8);
                    segXmodeWht[  segindex] = Integer.parseInt(strs[6]);
                    segXmodeCnt[  segindex] = Integer.parseInt(strs[7]);
                    segTrigForce[ segindex] = Integer.parseInt(strs[8]) + (Integer.parseInt(strs[9]) << 8);

                    if (segPatterns[segindex] > 0) segPatterns[segindex] -= 1; // device patterns start at 1

                    Log.v(LOGNAME, ">> Bright=" + curBright[segindex] + " Delay=" + curDelay[segindex]);
                    Log.v(LOGNAME, ">> Pattern=" + segPatterns[segindex] + " Mode=" + segXmodeEnb[segindex] + " Force=" + segTrigForce[segindex]);
                    Log.v(LOGNAME, ">> Hue=" + segXmodeHue[segindex] + " White=" + segXmodeWht[segindex] + " Count=" + segXmodeCnt[segindex]);

                    if (!CheckValue(curDelay[segindex], -rangeDelay, rangeDelay))
                    {
                        curDelay[segindex] = 0;
                        Log.v(LOGNAME, "Adjusting delay=" + curDelay[segindex]);
                    }

                    if (!CheckValue(curBright[segindex], 0, MAXVAL_PERCENT))
                    {
                        curBright[segindex] = MAXVAL_PERCENT;
                        Log.v(LOGNAME, "Adjusting bright=" + curBright[segindex]);
                    }

                    CheckSegVals(segindex);
                }
                else replyFail = true;
            }
            else replyFail = true;

            if (!replyFail)
            {
                if (--optionLines == 0) getSegments = false; // finished with segments
                else ++replyState;
            }
        }
        else if (getPatterns)
        {
            int index = (replyState-1)/3;
            if (index < customPatterns)
            {
                int line = ((replyState-1) % 3);

                     if (line == 0)     devPatternNames[index] = new String(reply);
                else if (line == 1)     devPatternHelp[index] = (new String(reply)).replace('\t', '\n');
                else if (!editPatterns) devPatternBits[index] = Integer.parseInt(reply, 16);
                else
                {
                    devPatternCmds[index] = new String(reply);
                    devPatternBits[index] = 0;

                    boolean haveforce = false;
                    String[] strs = reply.split("\\s+"); // remove ALL spaces

                    for (int i = 0; i < strs.length; ++i)
                    {
                        if (strs[i].length() <= 0) continue; // shouldn't happen?

                        if ((strs[i].charAt(0) == 'Q') && (strs[i].length() > 1))
                        {
                            int val = Integer.parseInt(strs[i].substring(1));
                            devPatternBits[index] |= val;
                        }
                        else if ((strs[i].charAt(0) == 'F') && (strs[i].length() > 1) && (strs[i].charAt(1) != '0')) // ignore zero-force setting
                            haveforce = true;

                        else if (strs[i].charAt(0) == 'I')
                        {
                            devPatternBits[index] |= 0x10;
                            if (haveforce) devPatternBits[index] |= 0x20;
                        }
                    }
                }

                if (--optionLines == 0) getPatterns = false; // finished with patterns
                else ++replyState;
            }
            else replyFail = true;
        }
        else if (getPlugins)
        {
            throw new NullPointerException("Custom Plugins Not Supported Yet");
        }
        else switch(replyState)
        {
            case 0: // first line: the title string (to sync with sequence)
            {
                if (reply.contains(TITLE_PIXELNUT))
                {
                    ++replyState;
                    progressPercent = 0;
                    progressPcentInc = 25;
                }
                else
                {
                    Log.w(LOGNAME, "Unexpected title: " + reply);
                    replyFail = true;
                }
                break;
            }
            case 1: // second line: number of additional lines + 6 device settings
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 7)
                {
                    optionLines     = Integer.parseInt(strs[0]);
                    numSegments     = Integer.parseInt(strs[1]);
                    segPatterns[0]  = Integer.parseInt(strs[2]);
                    customPatterns  = Integer.parseInt(strs[3]);
                    maxlenCmdStrs   = Integer.parseInt(strs[4]);
                    rangeDelay      = Integer.parseInt(strs[5]);
                    customPlugins   = Integer.parseInt(strs[6]);

                    if (numSegments < 0)
                    {
                        multiStrands = true;
                        numSegments = -numSegments;
                    }
                    else multiStrands = false;

                    if (segPatterns[0] > 0)
                    {
                        segPatterns[0] -= 1; // device patterns start at 1
                        initPatterns = false;
                    }
                    else initPatterns = true; // trigger sending initial pattern to device

                    if (customPatterns != 0) // indicates fixed internal device patterns
                    {
                        stdPatternsCount = 0; // prevent using patterns defined here

                        if (customPatterns < 0)
                        {
                            customPatterns =  -customPatterns;
                            editPatterns = false;
                        }
                    }
                    else stdPatternsCount = basicPatternsCount + advPatternsCount;

                    numPatterns = customPatterns + stdPatternsCount;

                    // if the command/pattern string is not long enough,
                    // then must only use the basic patterns
                    if (maxlenCmdStrs < (MINLEN_CMDSTR_PERSEG * numSegments))
                        useAdvPatterns = false;

                    Log.v(LOGNAME, ">> Option lines = " + optionLines);
                    Log.v(LOGNAME, ">> Segments=" + numSegments + ((numSegments > 1) ? (multiStrands ? " (physical)" : " (logical)") : ""));
                    Log.v(LOGNAME, ">> CurPattern=" + segPatterns[0] + " DoInit=" + initPatterns);
                    Log.v(LOGNAME, ">> CustomPatterns=" + customPatterns + " CanEdit=" + editPatterns);
                    Log.v(LOGNAME, ">> MaxCmdStr=" + maxlenCmdStrs + " AdvPatterns=" + useAdvPatterns);
                    Log.v(LOGNAME, ">> RangeDelay=" + rangeDelay + " XPlugins=" + customPlugins);
                    Log.v(LOGNAME, ">> Total patterns=" + numPatterns);

                    if (numSegments < 1) numSegments = 1;
                    if (customPlugins < 0) customPlugins = 0;

                    if (rangeDelay < MINVAL_DELAYRANGE)
                    {
                        rangeDelay = MINVAL_DELAYRANGE;
                        Log.v(LOGNAME, "Adjusting range=" + rangeDelay);
                    }

                    if (optionLines >= 1)
                    {
                        devPatternNames = new String[numPatterns];
                        devPatternHelp  = new String[numPatterns];
                        devPatternBits  = new int[numPatterns];

                        if (editPatterns) devPatternCmds = new String[numPatterns];
                    }
                    else replyFail = true;
                }
                else replyFail = true;

                if (!replyFail)
                {
                    progressPcentInc = 101/(optionLines+1);
                    Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);

                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            case 2: // third line: 5 more settings (if not multiple physical segments)
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 5)
                {
                    segPixels[0] = Integer.parseInt(strs[0]);
                    segLayers[0] = Integer.parseInt(strs[1]);
                    segTracks[0] = Integer.parseInt(strs[2]);
                    curBright[0] = Integer.parseInt(strs[3]);
                    curDelay[0]  = Integer.parseInt(strs[4]);

                    Log.v(LOGNAME, ">> Pixels=" + segPixels[0] + " Layers=" + segLayers[0] + " Tracks=" + segTracks[0]);
                    Log.v(LOGNAME, ">> Bright=" + curBright[0] + " Delay=" + curDelay[0]);

                    if (!CheckValue(curDelay[0], -rangeDelay, rangeDelay))
                    {
                        curDelay[0] = 0;
                        Log.v(LOGNAME, "Adjusting delay=" + curDelay[0]);
                    }

                    if (!CheckValue(curBright[0], 0, MAXVAL_PERCENT))
                    {
                        curBright[0] = MAXVAL_PERCENT;
                        Log.v(LOGNAME, "Adjusting bright=" + curBright[0]);
                    }

                    if (!CheckValue(segPixels[0], 1, 0) ||
                        !CheckValue(segLayers[0], 2, 0) ||
                        !CheckValue(segTracks[0], 1, 0))
                        replyFail = true;
                }
                else replyFail = true;

                if (!replyFail)
                {
                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            case 3: // fourth line: 5 extern mode values (if not multiple segments)
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 5)
                {
                    segXmodeEnb[0] = (Integer.parseInt(strs[0]) != 0);
                    segXmodeHue[0] = Integer.parseInt(strs[1]);
                    segXmodeWht[0] = Integer.parseInt(strs[2]);
                    segXmodeCnt[0] = Integer.parseInt(strs[3]);
                    segTrigForce[0] = Integer.parseInt(strs[4]);

                    Log.v(LOGNAME, ">> Enable=" + segXmodeEnb[0] + " Hue=" + segXmodeHue[0] + " White=" + segXmodeWht[0]);
                    Log.v(LOGNAME, ">> Count=" + segXmodeCnt[0] + " Force=" + segTrigForce[0]);

                    CheckSegVals(0);
                }
                else replyFail = true;

                if (!replyFail)
                {
                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            default: // ignore for forward compatibility
            {
                Log.w(LOGNAME, "Line=" + replyState + " Reply=" + reply);

                --optionLines;
                if (optionLines == 0) CheckForExtendedCommands();
                break;
            }
        }

        if (replyFail)
        {
            Log.e(LOGNAME, "Read failed: state=" + replyState);
            return -1;
        }
        else if ((replyState <= 1) || (optionLines != 0))
        {
            Log.v(LOGNAME, "Post progress...");
            return 1; // post progress
        }

        boolean moreinfo = false;

        if (setPercentage)
        {
            // use 101 to insure the progress bar fills up entirely
            progressPcentInc = 101.0 / ((getSegments ? (numSegments+1) : 0) + (customPatterns*3) + (customPlugins*2));
            progressPercent = 0;
            setPercentage = false;

            Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);
        }

        if (getSegments)
        {
            sendCmdStr = CMD_GET_SEGMENTS;
            optionLines = numSegments+1;
            moreinfo = true;
        }
        else if (getPatterns)
        {
            sendCmdStr = CMD_GET_PATTERNS;
            optionLines = customPatterns*3;
            moreinfo = true;
        }
        else if (getPlugins)
        {
            sendCmdStr = CMD_GET_PLUGINS;
            optionLines = customPlugins*2;
            moreinfo = true;
        }

        if (moreinfo)
        {
            replyState = 1;
            Log.v(LOGNAME, "Send new command...");
            return 2; // send new command
        }

        if (stdPatternsCount > 0)
        {
            int i;
            for (i = 0; i < basicPatternsCount; ++i)
            {
                devPatternNames[i] = basicPatternNames[i];
                devPatternHelp[ i] = basicPatternHelp[i];
                devPatternCmds[ i] = basicPatternCmds[i];
                devPatternBits[ i] = basicPatternBits[i];
            }

            int j = i;
            for (i = 0; i < advPatternsCount; ++i)
            {
                devPatternNames[i+j] = advPatternNames[i];
                devPatternHelp[ i+j] = advPatternHelp[i];
                devPatternCmds[ i+j] = advPatternCmds[i];
                devPatternBits[ i+j] = advPatternBits[i];
            }
        }

        didFinishReading = true;
        Log.v(LOGNAME, "Finished !!!");
        return 3; // finished - goto Controls activity
    }

    private boolean CheckValue(int val, int min, int max)
    {
        if (val < min) return false;
        if ((0 < max) && (max < val)) return false;
        return true;
    }
}