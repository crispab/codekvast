export class Application {
    dumpedAtMillis: number;
    invokedAtMillis: number;
    name: string;
    startedAtMillis: number;
    status: String;
    version: string

    // computed fields
    dumpedAt: Date;
    invokedAt: Date;
    startedAt: Date;
}
