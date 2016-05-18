export class Application implements Serializable<Application> {
    dumpedAtMillis: number;
    invokedAtMillis: number;
    name: string;
    startedAtMillis: number;
    status: String;
    version: string

    deserialize(input) {
        this.dumpedAtMillis = input.dumpedAtMillis;
        return this;
    }

}
