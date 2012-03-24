package org.eatabrick.vecna;

public class Entry {
  public String account  = "";
  public String user     = "";
  public String password = "";

  public Entry(String account, String user, String password) {
    this.account  = account;
    this.user     = user;
    this.password = password;
  }

  public Entry(String line) {
    String[] parts = line.split(" ");

    account  = parts[0];
    user     = parts[1];
    password = parts[2];
  }

  public String toLine() {
    return account + " " + user + " " + password;
  }
}
