
export interface Game {
    gameId: number;
    joinToken?: string;
    status: string; // "WAITING", "PLAYING", "FINISHED"
    boardState: string;
    playerBlack?: string;
    playerWhite?: string;
    winner?: string;
}