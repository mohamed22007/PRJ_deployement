export interface Move {
    moveId: number;
    player: string;
    path: number[][];
    isCapture: boolean;
    timestamp: string;
}