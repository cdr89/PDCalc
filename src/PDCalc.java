/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Domenico
 */

import java.io.IOException;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;


public class PDCalc extends MIDlet implements CommandListener {

    private Command exitCommand, submitCommand, setcoc, rep;
    private Display display;     // The display for this MIDlet

    private TextField dist, focale, cc;
    private ChoiceGroup aperture, unit, coc, fdist;
    private Form form, form1, form2;

    private Image logo;
    private ImageItem icon;

    private double f, d, cdc, di, iperfocale, ppv, ppl, pdc, das, dfas, fattconv=100, fattdist=1;

    private RecordStore rs;

    public PDCalc(){
        display = Display.getDisplay(this);

        try {
            logo = Image.createImage("/logo.png");
            icon = new ImageItem(null, logo, ImageItem.LAYOUT_LEFT, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        exitCommand = new Command("Esci", Command.EXIT, 0);
        submitCommand = new Command("Calcola", Command.OK, 0);
        setcoc = new Command("Imposta", Command.OK, 0);
        rep = new Command("Nuovo calcolo", Command.OK, 0);

        form = new Form("Calcolatore PDC");

        dist = new TextField("Distanza dal soggetto: ", "100.0", 10, TextField.DECIMAL);
        focale = new TextField("Lunghezza focale: [mm]", "50.0", 10, TextField.DECIMAL);

        cc = new TextField("Circolo di confusione: [micron]", "20.0", 5, TextField.DECIMAL);

        //lista unità di misura distanza
        fdist = new ChoiceGroup("", ChoiceGroup.POPUP);
        fdist.append("m", null);
        fdist.append("cm", null);
        fdist.append("mm", null);
        fdist.setSelectedIndex(1, true);

        //lista unità di misura output
        unit = new ChoiceGroup("Unità di misura: ", ChoiceGroup.POPUP);
        unit.append("m", null);
        unit.append("cm", null);
        unit.append("mm", null);
        unit.append("ft", null);
        unit.append("in", null);
        unit.setSelectedIndex(1, true);

        //lista circoli di confusione
        coc = new ChoiceGroup("Circolo di confusione: ", ChoiceGroup.POPUP);
        coc.append("Nikon DX", null);
        coc.append("Nikon FX", null);
        coc.append("Altro...", null);
        coc.setSelectedIndex(0, true);

        //lista aperture diaframma
        aperture = new ChoiceGroup("Diaframma: ", ChoiceGroup.POPUP);
        aperture.append("f/1",null);
        aperture.append("f/1.2",null);
        aperture.append("f/1.4",null);
        aperture.append("f/1.6",null);
        aperture.append("f/1.7",null);
        aperture.append("f/1.8",null);
        aperture.append("f/2",null);
        aperture.append("f/2.2",null);
        aperture.append("f/2.4",null);
        aperture.append("f/2.5",null);
        aperture.append("f/2.8",null);
        aperture.append("f/3.2",null);
        aperture.append("f/3.4",null);
        aperture.append("f/3.6",null);
        aperture.append("f/4",null);
        aperture.append("f/4.5",null);
        aperture.append("f/4.8",null);
        aperture.append("f/5",null);
        aperture.append("f/5.6",null);
        aperture.append("f/6.4",null);
        aperture.append("f/6.7",null);
        aperture.append("f/7.1",null);
        aperture.append("f/8",null);
        aperture.append("f/9",null);
        aperture.append("f/9.5",null);
        aperture.append("f/10",null);
        aperture.append("f/11",null);
        aperture.append("f/12.7",null);
        aperture.append("f/13.5",null);
        aperture.append("f/14.3",null);
        aperture.append("f/16",null);
        aperture.append("f/18",null);
        aperture.append("f/19",null);
        aperture.append("f/20",null);
        aperture.append("f/22",null);
        aperture.append("f/25",null);
        aperture.append("f/27",null);
        aperture.append("f/28",null);
        aperture.append("f/32",null);
        aperture.append("f/45",null);
        aperture.append("f/64",null);
        aperture.setSelectedIndex(30, true);

        form.append(icon);
        form.append(dist);
        form.append(fdist);
        form.append(focale);
        form.append(aperture);
        form.append(coc);
        form.append(unit);

        try {
            rs = RecordStore.openRecordStore("pdcalc", true);
        } catch (RecordStoreException ex) {
            ex.printStackTrace();
        }

        //tentativo di recupero dati da recordstore
        caricaDati();

    }

    public void startApp() {
        form.addCommand(exitCommand);
        form.addCommand(submitCommand);
        form.setCommandListener(this);

        display.setCurrent(form);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command c, Displayable s) {
        if (c == exitCommand) {
            try {
                rs.closeRecordStore();
            } catch (RecordStoreException ex) {
                ex.printStackTrace();
            }
            destroyApp(false);
            notifyDestroyed();
        }
        if(c==setcoc){
            salvaDati();
            calc();
        }
        if(c==submitCommand){
            if(coc.getString(coc.getSelectedIndex()).equals("Altro...")){
                form2 = new Form("Inserire circolo di confusione");
                form2.append(cc);
                form2.addCommand(exitCommand);
                form2.addCommand(setcoc);
                form2.setCommandListener(this);

                display.setCurrent(form2);
            }
            else{
                salvaDati();
                cocSelect();
                calc();
            }
        }
        if(c==rep)
            startApp();
    }

    public void calc(){
            f = Double.parseDouble(focale.getString());
            d = diaframma(aperture.getString(aperture.getSelectedIndex()));
            cdc = Double.parseDouble(cc.getString());
            fattconv = unita(unit.getString(unit.getSelectedIndex()));
            fattdist = unitadist(fdist.getString(fdist.getSelectedIndex()));
            di = fattdist*Double.parseDouble(dist.getString());
            iperfocale = (((f*f)/(d*cdc))+(f/1000));
            ppv = (((di/100)*(iperfocale-f/1000))/(iperfocale+(di/100)-(2*f/1000)));
            ppl = (((di/100)*(iperfocale-f/1000))/(iperfocale-(di/100)));
            pdc = (ppl-ppv);
            dfas = (di-ppv*100);
            das = (ppl*100-di);

            String um = unit.getString(unit.getSelectedIndex());

            form1 = new Form("Risultati");
            form1.append("Profondità di campo:\n"+format(pdc*fattconv)+' '+um+'\n');
            form1.append("Punto più vicino:\n"+format(ppv*fattconv)+' '+um+'\n');
            form1.append("Punto più lontano:\n"+format(ppl*fattconv)+' '+um+'\n');
            form1.append("Di fronte al soggetto:\n"+format(dfas*fattconv/100)+' '+um+'\n');
            form1.append("Dietro al soggetto:\n"+format(das*fattconv/100)+' '+um+'\n');
            form1.append("Iperfocale:\n"+format(iperfocale*fattconv)+' '+um+'\n');
            form1.addCommand(exitCommand);
            form1.addCommand(rep);
            form1.setCommandListener(this);

            display.setCurrent(form1);
    }

    public String format(double d){
        if(d<0)
            return "infinito";
        int temp = (int)(d*1000);
        if(temp%1000 < 10)
            return String.valueOf(temp/1000)+".00"+temp%1000;
         if(temp%1000 < 100)
            return String.valueOf(temp/1000)+".0"+temp%1000;
        return String.valueOf(temp/1000)+'.'+temp%1000;
    }

    public double unita(String s){
        if(s.equals("m")) return 1;
        if(s.equals("mm")) return 1000;
        if(s.equals("ft")) return 3.2808399;
        if(s.equals("in")) return 39.3700787;
        return 100;
    }

    public double unitadist(String s){
        if(s.equals("m")) return 100;
        if(s.equals("mm")) return 0.1;
        return 1;
    }


    public void cocSelect(){
        if(coc.getString(coc.getSelectedIndex()).equals("Nikon DX"))
            cc.setString("20.0");
        if(coc.getString(coc.getSelectedIndex()).equals("Nikon FX"))
            cc.setString("30.0");
        }

    //restituisce il valore diaframma
    public double diaframma(String f){
        if(f.equals("f/1")) return 1;
        if(f.equals("f/1.2")) return 1.189207;
        if(f.equals("f/1.4")) return 1.414214;
        if(f.equals("f/1.6")) return 1.587401;
        if(f.equals("f/1.7")) return 1.681793;
        if(f.equals("f/1.8")) return 1.781797;
        if(f.equals("f/2")) return 2;
        if(f.equals("f/2.2")) return 2.244924;
        if(f.equals("f/2.4")) return 2.378414;
        if(f.equals("f/2.5")) return 2.519842;
        if(f.equals("f/2.8")) return 2.828427;
        if(f.equals("f/3.2")) return 3.174802;
        if(f.equals("f/3.4")) return 3.363586;
        if(f.equals("f/3.6")) return 3.563595;
        if(f.equals("f/4")) return 4;
        if(f.equals("f/4.5")) return 4.489848;
        if(f.equals("f/4.8")) return 4.756828;
        if(f.equals("f/5")) return 5.039684;
        if(f.equals("f/5.6")) return 5.656854;
        if(f.equals("f/6.4")) return 6.349604;
        if(f.equals("f/6.7")) return 6.727171;
        if(f.equals("f/7.1")) return 7.127190;
        if(f.equals("f/8")) return 8;
        if(f.equals("f/9")) return 8.979696;
        if(f.equals("f/9.5")) return 9.513657;
        if(f.equals("f/10")) return 10.07937;
        if(f.equals("f/11")) return 11.313708;
        if(f.equals("f/12.7")) return 12.699208;
        if(f.equals("f/13.5")) return 13.454343;
        if(f.equals("f/14.3")) return 14.254379;
        if(f.equals("f/16")) return 16;
        if(f.equals("f/18")) return 17.959393;
        if(f.equals("f/19")) return 19.027314;
        if(f.equals("f/20")) return 20.158737;
        if(f.equals("f/22")) return 22.627417;
        if(f.equals("f/25")) return 25.398417;
        if(f.equals("f/27")) return 26.908685;
        if(f.equals("f/28")) return 28.508759;
        if(f.equals("f/32")) return 32;
        if(f.equals("f/45")) return 45.254834;
        return 64.0;
    }



    public void salvaDati(){
        writeString(dist.getString(), 1);
        writeString(focale.getString(), 2);
        writeString(cc.getString(), 3);
        writeString(""+aperture.getSelectedIndex(), 4);
        writeString(""+unit.getSelectedIndex(), 5);
        writeString(""+coc.getSelectedIndex(), 6);
        writeString(""+fdist.getSelectedIndex(), 7);
    }

    public void caricaDati(){
        try {
            if (rs==null || rs.getNumRecords() < 7) {
                return;
            }
        } catch (RecordStoreNotOpenException ex) {
            ex.printStackTrace();
        }
        dist.setString(readString(1));
        focale.setString(readString(2));
        cc.setString(readString(3));
        aperture.setSelectedIndex(Integer.parseInt(readString(4)), true);
        unit.setSelectedIndex(Integer.parseInt(readString(5)), true);
        coc.setSelectedIndex(Integer.parseInt(readString(6)), true);
        fdist.setSelectedIndex(Integer.parseInt(readString(7)), true);
    }

    public void writeString(String s, int index){
        byte[] b = s.getBytes();
        if (rs != null){
            try {
                if(rs.getNumRecords()<7)
                    rs.addRecord(b, 0, b.length);
                else
                    rs.setRecord(index, b, 0, b.length);
            } catch (RecordStoreException ex) {
                ex.printStackTrace();
            }
        }
    }

    public String readString(int index){
        String s = "";
        byte[] b = null;
        try {
            b = rs.getRecord(index);
        } catch (RecordStoreException ex) {
                ex.printStackTrace();
        }
        if(b!=null)
            s = new String(b);
        if(s.equals("")){
            switch(index){
                case 1: dist.setString("100.0");
                case 2: focale.setString("50.0");
                case 3: cc.setString("20.0");
                case 4: aperture.setSelectedIndex(30, true);
                case 5: unit.setSelectedIndex(1,true);
                case 6: coc.setSelectedIndex(0,true);
                case 7: fdist.setSelectedIndex(1,true);
            }
        }
        return s;
    }

}