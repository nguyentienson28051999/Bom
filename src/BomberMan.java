import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
class Bomb {
    int x, y;
    boolean exploded;
    int countToExplode, intervalToExplode = 4;// đếm số nhịp - số nhịp để bomb nổ
}

public class BomberMan extends JPanel implements Runnable, KeyListener {

    boolean isRunning;
    // Đa luồng
    Thread thread;
    BufferedImage view, concreteTile, blockTile, player;

    Bomb bomb;
    int[][] scene;
    int playerX, playerY;
    int tileSize = 16, rows = 13, columns = 15;
    int speed = 4;
    boolean right, left, up, down;
    boolean moving;
    int framePlayer = 0, intervalPlayer = 5, indexAnimPlayer = 0;
    BufferedImage[] playerAnimUp, playerAnimDown, playerAnimRight, playerAnimLeft;
    int frameBomb = 0, intervalBomb = 7, indexAnimBomb = 0;
    BufferedImage[] bombAnim;
    BufferedImage[] fontExplosion, rightExplosion, leftExplosion, upExplosion, downExplosion;
    int frameExplosion = 0, intervalExplosion = 3, indexAnimExplosion = 0;//
    BufferedImage[] concreteExploding;
    int frameConcreteExploding = 0, intevalConcreteExploding = 4, indexConcreteExploding = 0;
    boolean concreteAnim = false;
    int bombX, bombY;

    final int SCALE = 3; // Tăng kích thước trò chơi
    final int WIDTH = (tileSize * SCALE) * columns; // Chiều rộng
    final int HEIGHT = (tileSize * SCALE) * rows; // Chiều cao

    public BomberMan() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addKeyListener(this);
    }

    public static void main(String[] args) {
        JFrame w = new JFrame("Bomberman");
        w.setResizable(false);
        w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        w.add(new BomberMan());
        w.pack();
        w.setLocationRelativeTo(null);
        w.setVisible(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this);
            isRunning = true;
            thread.start();
        }
    }

    // Kiểm tra 4 góc ảnh người chơi ở vị trí tiếp theo xem có va vào gạch hay tường ko
    public boolean isFree(int nextX, int nextY) {
        int size = SCALE * tileSize;
        // góc trên trái
        int nextX_1 = nextX / size;
        int nextY_1 = nextY / size;
        // góc trên phải
        int nextX_2 = (nextX + size - 1) / size;
        int nextY_2 = nextY / size;
        //góc dưới trái
        int nextX_3 = nextX / size;
        int nextY_3 = (nextY + size - 1) / size;
        // góc dưới phải
        int nextX_4 = (nextX + size - 1) / size;
        int nextY_4 = (nextY + size - 1) / size;

        // trả về Fale nếu 1 trong 4 góc va vào tưởng hoặc gạch
        return !((scene[nextY_1][nextX_1] == 1 || scene[nextY_1][nextX_1] == 2) ||
                (scene[nextY_2][nextX_2] == 1 || scene[nextY_2][nextX_2] == 2) ||
                (scene[nextY_3][nextX_3] == 1 || scene[nextY_3][nextX_3] == 2) ||
                (scene[nextY_4][nextX_4] == 1 || scene[nextY_4][nextX_4] == 2));
    }

    // Lấy hình ảnh + Vẽ map
    public void start() {
        try {
            view = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

            // Lấy ảnh và lưu dưới dạng BufferedImage
            BufferedImage spriteSheet = ImageIO.read(getClass().getResource("/sheets.png"));

            concreteTile = spriteSheet.getSubimage(4 * tileSize, 3 * tileSize, tileSize, tileSize); // Lấy hình gạch
            blockTile = spriteSheet.getSubimage(3 * tileSize, 3 * tileSize, tileSize, tileSize); // Lấy hình tường
            player = spriteSheet.getSubimage(4 * tileSize, 0, tileSize, tileSize); // Ảnh người chơi

            playerAnimUp = new BufferedImage[3]; // người chơi lên
            playerAnimDown = new BufferedImage[3]; //
            playerAnimRight = new BufferedImage[3];//
            playerAnimLeft = new BufferedImage[3];//
            bombAnim = new BufferedImage[3]; // Quả bomb
            fontExplosion = new BufferedImage[4]; // lấy ảnh nổ
            rightExplosion = new BufferedImage[4];//
            leftExplosion = new BufferedImage[4];//
            upExplosion = new BufferedImage[4];//
            downExplosion = new BufferedImage[4];//
            concreteExploding = new BufferedImage[6];//

            for (int i = 0; i < 6; i++) {
                concreteExploding[i] = spriteSheet.getSubimage((i + 5) * tileSize, 3 * tileSize, tileSize, tileSize);
            }

            fontExplosion[0] = spriteSheet.getSubimage(2 * tileSize, 6 * tileSize, tileSize, tileSize);
            fontExplosion[1] = spriteSheet.getSubimage(7 * tileSize, 6 * tileSize, tileSize, tileSize);
            fontExplosion[2] = spriteSheet.getSubimage(2 * tileSize, 11 * tileSize, tileSize, tileSize);
            fontExplosion[3] = spriteSheet.getSubimage(7 * tileSize, 11 * tileSize, tileSize, tileSize);

            rightExplosion[0] = spriteSheet.getSubimage(4 * tileSize, 6 * tileSize, tileSize, tileSize);
            rightExplosion[1] = spriteSheet.getSubimage(9 * tileSize, 6 * tileSize, tileSize, tileSize);
            rightExplosion[2] = spriteSheet.getSubimage(4 * tileSize, 11 * tileSize, tileSize, tileSize);
            rightExplosion[3] = spriteSheet.getSubimage(9 * tileSize, 11 * tileSize, tileSize, tileSize);

            leftExplosion[0] = spriteSheet.getSubimage(0, 6 * tileSize, tileSize, tileSize);
            leftExplosion[1] = spriteSheet.getSubimage(5 * tileSize, 6 * tileSize, tileSize, tileSize);
            leftExplosion[2] = spriteSheet.getSubimage(0, 11 * tileSize, tileSize, tileSize);
            leftExplosion[3] = spriteSheet.getSubimage(5 * tileSize, 11 * tileSize, tileSize, tileSize);

            upExplosion[0] = spriteSheet.getSubimage(2 * tileSize, 4 * tileSize, tileSize, tileSize);
            upExplosion[1] = spriteSheet.getSubimage(7 * tileSize, 4 * tileSize, tileSize, tileSize);
            upExplosion[2] = spriteSheet.getSubimage(2 * tileSize, 9 * tileSize, tileSize, tileSize);
            upExplosion[3] = spriteSheet.getSubimage(7 * tileSize, 9 * tileSize, tileSize, tileSize);

            downExplosion[0] = spriteSheet.getSubimage(2 * tileSize, 8 * tileSize, tileSize, tileSize);
            downExplosion[1] = spriteSheet.getSubimage(7 * tileSize, 8 * tileSize, tileSize, tileSize);
            downExplosion[2] = spriteSheet.getSubimage(2 * tileSize, 13 * tileSize, tileSize, tileSize);
            downExplosion[3] = spriteSheet.getSubimage(7 * tileSize, 13 * tileSize, tileSize, tileSize);

            for (int i = 0; i < 3; i++) {
                playerAnimLeft[i] = spriteSheet.getSubimage(i * tileSize, 0, tileSize, tileSize);
                playerAnimRight[i] = spriteSheet.getSubimage(i * tileSize, tileSize, tileSize, tileSize);
                playerAnimDown[i] = spriteSheet.getSubimage((i + 3) * tileSize, 0, tileSize, tileSize);
                playerAnimUp[i] = spriteSheet.getSubimage((i + 3) * tileSize, tileSize, tileSize, tileSize);
                bombAnim[i] = spriteSheet.getSubimage(i * tileSize, 3 * tileSize, tileSize, tileSize);
            }

            // Khởi tạo map 1 là tưởng, 0 là đường đi
            scene = new int[][]{
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            };

            // Random các số 2 ở vị trí bằng 0 là gạch
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (scene[i][j] == 0) {
                        if (new Random().nextInt(10) < 5) {
                            scene[i][j] = 2;
                        }
                    }
                }
            }
            // Để vị trí trống cho người chơi
            scene[1][1] = 0;
            scene[2][1] = 0;
            scene[1][2] = 0;

            playerX = (tileSize * SCALE);
            playerY = (tileSize * SCALE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {
        moving = false;
        // Kiểm tra trạng thái di chuyển (right,left,up,down) nếu mà free thì tăng vị trí của người và cho phép di chuyển
        if (right && isFree(playerX + speed, playerY)) {
            playerX += speed;
            moving = true;
        }
        if (left && isFree(playerX - speed, playerY)) {
            playerX -= speed;
            moving = true;
        }
        if (up && isFree(playerX, playerY - speed)) {
            playerY -= speed;
            moving = true;
        }
        if (down && isFree(playerX, playerY + speed)) {
            playerY += speed;
            moving = true;
        }

        if (bomb != null) {// Nếu có bomb thì tăng frameBomb
            frameBomb++;
            if (frameBomb == intervalBomb) {// Nếu framebome bằng khoảng đếm ngắt của bomb thì chạy code ở dưới
                frameBomb = 0; // gán frameBomb bằng 0
                indexAnimBomb++; // đếm nhịp thở của bomb tăng 1
                if (indexAnimBomb > 2) { // nếu nhịp thở >2 thì cho về 0 và đếm nhịp nổ tăng 1
                    indexAnimBomb = 0;
                    bomb.countToExplode++;
                }
                if (bomb.countToExplode >= bomb.intervalToExplode) { // Nếu nhịp nổ bằng nhịp nổ quy định thì cho bomb nổ và chuyển concreteAnim = True (gạch nổ)
                    concreteAnim = true;
                    bombX = bomb.x;
                    bombY = bomb.y;
                    bomb.exploded = true;

                    //Nếu bomb nổ mà xung quanh là gạch thì sẽ chuyển thành gạch nổ
                    if (scene[bomb.y + 1][bomb.x] == 2) {
                        scene[bomb.y + 1][bomb.x] = -1;
                    }
                    if (scene[bomb.y - 1][bomb.x] == 2) {
                        scene[bomb.y - 1][bomb.x] = -1;
                    }
                    if (scene[bomb.y][bomb.x + 1] == 2) {
                        scene[bomb.y][bomb.x + 1] = -1;
                    }
                    if (scene[bomb.y][bomb.x - 1] == 2) {
                        scene[bomb.y][bomb.x - 1] = -1;
                    }
                }
            }

            if(bomb.exploded) { // Nếu bomb nổ
                frameExplosion++; // Đếm frame nổ
                if (frameExplosion == intervalExplosion) { // nếu frame nổ bằng frame nổ quy định
                    frameExplosion = 0;// gán frame nổ bằng 0
                    indexAnimExplosion++; // vị trí ảnh nổ tăng lên 1
                    if (indexAnimExplosion == 4) {
                        // nếu ảnh nổ bằng 4 thì đổi thành 0 đổi bomb thành đường đi và xóa bomb
                        indexAnimExplosion = 0;
                        scene[bomb.y][bomb.x] = 0;
                        bomb = null;
                    }
                }
            }
        }

        if (concreteAnim) {  // Nếu trạng thái là gạch nổ
            frameConcreteExploding++; // tăng frame gạch nổ lên 1
            if (frameConcreteExploding == intevalConcreteExploding) { // nếu frame gạch nổ bằng frame gạch nổ quy đihj
                frameConcreteExploding = 0; // gán frame gạch nổ bằng 0
                indexConcreteExploding++; // Tăng vị trí gạch nổ lên 1
                if (indexConcreteExploding == 5) {// nếu gạch nổ đến ảnh số 5 thì chugs ta cho khung hình về ảnh số 1
                    indexConcreteExploding = 0;
                    // Nếu nó đang là gạch nổ thì chuyển nó thành đường
                    if (scene[bombY + 1][bombX] == -1) {
                        scene[bombY + 1][bombX] = 0;
                    }
                    if (scene[bombY - 1][bombX] == -1) {
                        scene[bombY - 1][bombX] = 0;
                    }
                    if (scene[bombY][bombX + 1] == -1) {
                        scene[bombY][bombX + 1] = 0;
                    }
                    if (scene[bombY][bombX - 1] == -1) {
                        scene[bombY][bombX - 1] = 0;
                    }
                    concreteAnim = false; // Gán trạng thái gạch nổ bằng False
                }
            }
        }

        if (moving) {
            //Nếu người chơi di chuyển thì framePlayer tăng liên tục từ 0->2
            framePlayer++;
            if (framePlayer > intervalPlayer) {
                framePlayer = 0;
                indexAnimPlayer++;
                if (indexAnimPlayer > 2) {
                    indexAnimPlayer = 0;
                }
            }
            // Tùy vào trạng thái di chuyển thì chọn mảng hình ảnh phù hợp
            if (right) {
                player = playerAnimRight[indexAnimPlayer];
            } else if (left) {
                player = playerAnimLeft[indexAnimPlayer];
            } else if (up) {
                player = playerAnimUp[indexAnimPlayer];
            } else if (down) {
                player = playerAnimDown[indexAnimPlayer];
            }
        } else {
            // Nếu không di cuyển thì chọn ảnh quay lại màn hình hay chính là ảnh xuống ở vị trí thứ 2
            player = playerAnimDown[1];
        }
    }

    // Dựa vào bản đồ game để vẽ người chơi và các đối tượng trong game
    public void draw() {
        Graphics2D g2 = (Graphics2D) view.getGraphics();
        g2.setColor(new Color(56, 135, 0));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        int size = tileSize * SCALE;
        //Kiểm tra giá trinh trong mảng bản đồ nếu là 0 -> đường, 1 -> tưởng, 2 -> gạch, 3 -> bomb, -1 -> gạch vỡ
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                if (scene[j][i] == 1) {
                    g2.drawImage(blockTile, i * size, j * size, size, size, null);
                } else if (scene[j][i] == 2) {
                    g2.drawImage(concreteTile, i * size, j * size, size, size, null);
                } else if (scene[j][i] == 3) {
                    if (bomb != null) {
                        if (bomb.exploded) {
                            g2.drawImage(fontExplosion[indexAnimExplosion], bomb.x * size, bomb.y * size, size, size, null);
                            if (scene[bomb.y][bomb.x + 1] == 0) {
                                g2.drawImage(rightExplosion[indexAnimExplosion], (bomb.x + 1) * size, bomb.y * size, size, size, null);
                            }
                            if (scene[bomb.y][bomb.x - 1] == 0) {
                                g2.drawImage(leftExplosion[indexAnimExplosion], (bomb.x - 1) * size, bomb.y * size, size, size, null);
                            }
                            if (scene[bomb.y - 1][bomb.x] == 0) {
                                g2.drawImage(upExplosion[indexAnimExplosion], bomb.x * size, (bomb.y - 1) * size, size, size, null);
                            }
                            if (scene[bomb.y + 1][bomb.x] == 0) {
                                g2.drawImage(downExplosion[indexAnimExplosion], bomb.x * size, (bomb.y + 1) * size, size, size, null);
                            }
                        } else {
                            g2.drawImage(bombAnim[indexAnimBomb], i * size, j * size, size, size, null);
                        }
                    }
                }  else if (scene[j][i] == -1) {
                    g2.drawImage(concreteExploding[indexConcreteExploding], i * size, j * size, size, size, null);
                }
            }
        }

        // Vẽ người chơi
        g2.drawImage(player, playerX, playerY, size, size, null);

        Graphics g = getGraphics();
        g.drawImage(view, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
    }

    @Override
    public void run() {
        try {
            // Nhận lệnh từ bàn phím
            requestFocus();
            start();
            while (isRunning) {
                update();
                draw();
                // Tốc độ của trò chơi
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    // Xử lý sự kiện bàn phím khi ấn
    @Override
    public void keyPressed(KeyEvent e) {
        // Nếu ấn phím space thì kiểm tra nếu hiện tại ko có bomb trong bản đồ thì tạo bom mới ở ô người chơi đang dứng
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (bomb == null) {
                bomb = new Bomb();
                bomb.x = (playerX + ((SCALE * tileSize) / 2)) / (SCALE * tileSize);
                bomb.y = (playerY + ((SCALE * tileSize) / 2)) / (SCALE * tileSize);
                scene[bomb.y][bomb.x] = 3;// 3 là ý hiệu của bomb trong bản đồ
            }
        }
        // Nhận giá trị từ bàn phím xác định hướng đi của người chơi
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            right = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            left = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            up = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            down = true;
        }
    }

    // Sử lý khi nhấc tay khỏi bàn phím
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            right = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            left = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            up = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            down = false;
        }
    }
}
