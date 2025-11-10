package com.tetris.controller;

import org.junit.jupiter.api.Test;

import com.comp2042.controller.ClassicBattle;

import static org.junit.jupiter.api.Assertions.*;

class ClassicBattleLogicTest {

    @Test
    void computeWinnerInfo_leftWins() {
        ClassicBattle.WinnerInfo w = ClassicBattle.computeWinnerInfo(10, 5);
        assertEquals("Left Player Wins!", w.title());
        assertTrue(w.reason().toLowerCase().contains("higher score") || w.reason().toLowerCase().contains("survival") || !w.reason().toLowerCase().contains("tie"));
    }

    @Test
    void computeWinnerInfo_rightWins() {
        ClassicBattle.WinnerInfo w = ClassicBattle.computeWinnerInfo(2, 7);
        assertEquals("Right Player Wins!", w.title());
        assertTrue(w.reason().toLowerCase().contains("higher score") || w.reason().toLowerCase().contains("survival") || !w.reason().toLowerCase().contains("tie"));
    }

    @Test
    void computeWinnerInfo_draw() {
        ClassicBattle.WinnerInfo w = ClassicBattle.computeWinnerInfo(4, 4);
        assertEquals("Draw!", w.title());
        assertTrue(w.reason().toLowerCase().contains("tie") || w.reason().toLowerCase().contains("same score"));
    }

    @Test
    void formatTime_various() {
        assertEquals("00:00", ClassicBattle.formatTime(0));
        assertEquals("00:01", ClassicBattle.formatTime(1));
        assertEquals("00:59", ClassicBattle.formatTime(59));
        assertEquals("01:00", ClassicBattle.formatTime(60));
        assertEquals("01:05", ClassicBattle.formatTime(65));
        assertEquals("10:00", ClassicBattle.formatTime(600));
    }
}
