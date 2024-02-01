import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BlackJack {
	private class Card {
		String value;
		String type;

		Card(String value, String type) {
			this.value = value;
			this.type = type;
		}

		public String toString() {
			return value + " of " + type;
		}

		public int getValue() {
			if ("AceJackQueenKing".contains(value)) { // Ace Jack Queen King
				if (value == "Ace") {
					return 11;
				}
				return 10;
			}
			return Integer.parseInt(value); // 2-10
		}

		public boolean isAce() {
			return value == "Ace";
		}

		public String getImagePath() {
			return "cards/" + toString() + ".png";
		}
	}

	private Map<String, Integer> playerChipsMap = new HashMap<>(); // Initialize the playerChipsMap
	private String currentPlayerUsername; // Current player's username

	ArrayList<Card> deck;
	Random random = new Random(); // shuffle deck

	// dealer
	Card hiddenCard; // the dealer's hidden card
	ArrayList<Card> dealerHand; // what cards the dealer has
	int dealerHandSum; // the sum of the cards in the dealers hand
	int dealerAceCount; // the count of aces in the dealers hand

	// player
	ArrayList<Card> playerHand; // what cards the player has
	int playerHandSum; // the sum of the cards in the players hand
	int playerAceCount; // the count of aces in the players hand
	int playerChips; // the total count of the player's chips
	int playerBet; // the amount of chips the user has bet on the hand
	boolean playerFound = false; //if the player is found in the text file

	// window
	int boardWidth = 1000; // set the width of the board
	int boardHeight = 600; // set the height of the board

	int cardWidth = 110; // set the width of the cards
	int cardHeight = 154; // set the height of the cards

	JFrame frame = new JFrame("Black Jack");
	JPanel gamePanel = new JPanel() {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			try {
				// draw the hidden card
				Image hiddenCardImage = new ImageIcon(getClass().getResource("cards/BACK.png")).getImage();
				if (!standButton.isEnabled()) {
					hiddenCardImage = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
				}
				g.drawImage(hiddenCardImage, 20, 20, cardWidth, cardHeight, null);

				// draw the dealer's cards
				for (int i = 0; i < dealerHand.size(); i++) {
					Card card = dealerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, cardWidth + 25 + (cardWidth + 5) * i, 20, cardWidth, cardHeight, null);
				}

				// draw the player's cards
				for (int i = 0; i < playerHand.size(); i++) {
					Card card = playerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, 20 + (cardWidth + 5) * i, 320, cardWidth, cardHeight, null);
				}
				
				String userHand = "You have " + playerHandSum;
				
				g.setFont(new Font("Arial", Font.PLAIN, 20));
				g.setColor(Color.white);
				g.drawString(userHand, 25, 500);

				//handle game ending and player's bets
				if (!standButton.isEnabled()) {
					dealerHandSum = reduceDealerAce();
					playerHandSum = reducePlayerAce();

					String gameOverText = endGameText();;

					g.setFont(new Font("Arial", Font.PLAIN, 30));
					g.setColor(Color.white);
					g.drawString(gameOverText, 150, 250);

					endGame();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	JMenuBar menuBar = new JMenuBar(); //create a menu bar
	JMenu rulesMenu = new JMenu("Rules"); //create a menu
	JMenuItem tutorialItem = new JMenuItem("How to Play"); //create an item for how to play
	JMenuItem bettingItem = new JMenuItem("Betting and Payout"); //create an item for the betting rules and the payouts 
	
	JPanel buttonPanel = new JPanel(); // create a panel for the buttons
	JButton hitButton = new JButton("Hit"); // create a button for the player to hit
	JButton standButton = new JButton("Stand"); // create a button for the player to stay
	JButton doubleDownButton = new JButton("Double Down"); //create a button so the player can double down
	JButton chipsIndicator = new JButton("Show chip count"); //create a button to display the user's chip total
	JButton playAgainButton = new JButton("Play Again"); //create a button so you can play again

	private static final String CHIP_TOTAL_FILE = "src/chip_total.txt";

	BlackJack() {
		loadChipTotalFromFile(); // Load chip total from file for all existing users
		showUsernameInput(); // Ask for username

		//setup the JMenu 
		frame.setJMenuBar(menuBar);
		menuBar.add(rulesMenu);
		rulesMenu.add(tutorialItem);
		rulesMenu.add(bettingItem);
		
		// set the attributes of the frame
		frame.setVisible(true);
		frame.setSize(boardWidth, boardHeight);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// set the attributes of the game panel
		gamePanel.setLayout(new BorderLayout());
		gamePanel.setBackground(new Color(53, 101, 77));
		frame.add(gamePanel);

		//set attributes for the hit, stay, setBet, playAgain, and chipsIndicator button and the bet text field
		hitButton.setFocusable(false);
		buttonPanel.add(hitButton);
		standButton.setFocusable(false);
		buttonPanel.add(standButton);
		doubleDownButton.setFocusable(false);
		buttonPanel.add(doubleDownButton);
		chipsIndicator.setFocusable(false);
		buttonPanel.add(chipsIndicator);
		buttonPanel.add(playAgainButton);
		playAgainButton.setFocusable(false);
		playAgainButton.setEnabled(false); // Initially, disable the "Play Again" button
		frame.add(buttonPanel, BorderLayout.SOUTH);
		
		 tutorialItem.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                showTutorialPopup();
	            }
	        });
		 bettingItem.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                showBettingPopup();
	            }
	        });

		// handle when the player presses the hit button
		hitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doubleDownButton.setEnabled(false);
				Card card = deck.remove(deck.size() - 1); // get the card the player will get from hitting
				playerHandSum += card.getValue(); // get the value of the card
				playerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase playerAceCount by 1
				playerHand.add(card); // add the card to the player's hand
				if (reducePlayerAce() > 21) { // if the player is over 21 but has an ace reduce the value from 11 to 1
					hitButton.setEnabled(false);
				}
				gamePanel.repaint();
			}
		});

		// handle when the player presses the stay button
		standButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hitButton.setEnabled(false);
				standButton.setEnabled(false);
				doubleDownButton.setEnabled(false);

				while (dealerHandSum < 17 ) { // the dealer will only get another card if their card total is below 17
					Card card = deck.remove(deck.size() - 1); // get the card the dealer will get
					dealerHandSum += card.getValue(); // get the value of the card
					dealerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase dealerAceCount by 1
					dealerHand.add(card); // add the card to the dealer's hand
				}

				gamePanel.repaint();
			}
		});

		//handle when the player presses the playAgain button
		playAgainButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetGame(); //reset the game so the user can play again
			}
		});

		//handle when the player presses the doubleDown button
		doubleDownButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doubleDownButton.setEnabled(false);
				
				if(playerBet * 2 < playerChips) {
					playerBet *= 2; //double the user's bet
					
					//give the user their one card
					Card card = getCard();
					playerHandSum += card.getValue();
					playerAceCount += card.isAce() ? 1 : 0;
					playerHand.add(card);
					
					hitButton.setEnabled(false);
				} else {
					JOptionPane.showMessageDialog(null, "Not enough chips to double down", "Invalid Input", JOptionPane.ERROR_MESSAGE);
				}
				
				gamePanel.repaint();
			}
		});

		//handle when the player presses the playAgain button
		chipsIndicator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "You have " + playerChips + " remaining");
			}
		});

		gamePanel.repaint();
	}
	
	public void saveChipTotalToFile() {
		playerChipsMap.put(currentPlayerUsername, playerChips); //most updated chip total for the player
		// Append chip balances for all players
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHIP_TOTAL_FILE, false))) {
			// Append chip balance for the current player
			for (Map.Entry<String, Integer> entry : playerChipsMap.entrySet()) {
				writer.write(entry.getKey() + ":" + entry.getValue());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadChipTotalFromFile() {
		// Load chip balances for all players
		try (BufferedReader reader = new BufferedReader(new FileReader(CHIP_TOTAL_FILE))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(":");
				if (parts.length == 2) {
					String username = parts[0];
					int chips = Integer.parseInt(parts[1]);
					playerChipsMap.put(username, chips);
				}
			}
		} catch (IOException  ioe) {
			ioe.printStackTrace();
		}
	}
	
	//create the popup for when the user click the bettingItem
	private void showBettingPopup() {
        // Create a panel for the rules popup
        JPanel bettingPanel = new JPanel();
        bettingPanel.setLayout(new BorderLayout());

        // Add JTextArea to display rules
        JTextArea bettingTextArea = new JTextArea();
        bettingTextArea.setText("Rules of Betting in Blackjack:\n\n" +
                "1. Player must bet before they receive their cards.\n" +
        		"2. Player must bet above 10 chips and below 50,000 chips.\n" +
                "3. If the player doubles down the bet for that hand is doubled.\n" +
        		"4. If the player has blackjack, they receive a payout of 3:2 on their original bet.\n" +
                "5. If the player wins, they receive a payout of 1:1 on their original bet.\n "
        		);

        // Make the text area not editable
        bettingTextArea.setEditable(false);

        // Add the text area to the panel
        bettingPanel.add(new JScrollPane(bettingTextArea), BorderLayout.CENTER);

        // Create and show the popup
        JOptionPane.showMessageDialog(frame, bettingPanel, "Blackjack Rules", JOptionPane.INFORMATION_MESSAGE);
    }
	
	////create the popup for when the user click the tutoritalItem
	private void showTutorialPopup() {
        // Create a panel for the rules popup
        JPanel tutorialPanel = new JPanel();
        tutorialPanel.setLayout(new BorderLayout());

        // Add JTextArea to display rules
        JTextArea tutorialTextArea = new JTextArea();
        tutorialTextArea.setText("Rules of Blackjack:\n\n" +
                "1. The goal is to beat the dealer by having a hand value closer to 21 without going over.\n" +
                "2. Number cards are worth their face value; face cards are worth 10 points; and an Ace can be worth 1 or 11.\n" +
                "3. Players are dealt two cards, and they can choose to 'Hit' to get another card or 'Stand' to keep their current hand.\n" +
                "4. 'Double Down' allows players to double their initial bet and receive only one additional card.\n" +
                "5. The dealer must hit until their hand value is 17 or higher.\n" +
                "6. If a player's hand exceeds 21, they bust and lose the round.\n" +
                "7. The player with a hand value closest to 21 without going over wins.\n" +
                "8. Chip totals are used for betting, and the game keeps track of each player's total chips.\n");

        // Make the text area not editable
        tutorialTextArea.setEditable(false);

        // Add the text area to the panel
        tutorialPanel.add(new JScrollPane(tutorialTextArea), BorderLayout.CENTER);

        // Create and show the popup
        JOptionPane.showMessageDialog(frame, tutorialPanel, "Blackjack Rules", JOptionPane.INFORMATION_MESSAGE);
    }

	//get the player's username
	private void showUsernameInput() {
		currentPlayerUsername = JOptionPane.showInputDialog(null, "Enter your username:");
		if (currentPlayerUsername == null || currentPlayerUsername.trim().isEmpty()) {
			System.exit(0); // Exit the game if the user cancels or enters an empty username
		}

		// Load or create chip balance for the current player
		if (playerChipsMap.containsKey(currentPlayerUsername)) {
			playerChips = playerChipsMap.get(currentPlayerUsername);
			playerFound = true;
		} else {
			playerChips = 0; // Set an initial balance for new players
			playerFound = false;
		}
		startGame(); // Start the game after obtaining the username
	}

	//start the game
	public void startGame() {
		System.out.println("Starting the game...");
		if (playerChips == 0) { 
			showDepositPanel(); //ask the user if they want to deposti chips or not
		}
		else {
			setUserBet(); //let the user set their bet if they already have chips
		}

		// deck
		buildDeck(); //build the deck of cards
		shuffleDeck(); //shuffle the deck of cards
		if(playerBet == 0) {
			setUserBet(); //set the user's bet for the hand
		}

		// dealer
		dealerHand = new ArrayList<Card>();
		dealerHandSum = 0; // initialize the sum of the dealer's cards
		dealerAceCount = 0; // initialize the count of aces in the dealer's hand

		// get the dealer's hidden card
		hiddenCard = getCard(); // get the hidden card
		dealerHandSum += hiddenCard.getValue();
		dealerAceCount += hiddenCard.isAce() ? 1 : 0;

		// get the dealer's one face up card
		Card card = getCard();
		dealerHandSum += card.getValue();
		dealerAceCount += card.isAce() ? 1 : 0;
		dealerHand.add(card);

		// player
		playerHand = new ArrayList<Card>();
		playerHandSum = 0; // initialize the sum of the player's cards
		playerAceCount = 0; // initialize the count of aces in the player's hand

		// give the player their two starting cards
		for (int i = 0; i < 2; i++) {
			card = getCard();
			playerHandSum += card.getValue();
			playerAceCount += card.isAce() ? 1 : 0;
			playerHand.add(card);
		}
	}
	
	//get a card from the deck
	public Card getCard() {
		return deck.remove(deck.size() - 1);
	}

	//end the current game which allows the user to play again
	public void endGame() {
		playAgainButton.setEnabled(true); // Enable the "Play Again" button when the game ends
	}

	//reset the game so the user plays again
	public void resetGame() {
		playAgainButton.setEnabled(false); // Disable the "Play Again" button after resetting the game
		playerHand.clear();
		playerHandSum = 0;
		playerAceCount = 0;
		playerBet = 0;

		dealerHand.clear();
		dealerHandSum = 0;
		dealerAceCount = 0;

		// Enable hit, stand, and double down buttons
		hitButton.setEnabled(true);
		standButton.setEnabled(true);
		doubleDownButton.setEnabled(true);

		startGame(); // Start a new game
		gamePanel.repaint();
	}

	//let a user deposit chips into their account
	public void depositChips() {
		boolean isValidDeposit = false;

		while (!isValidDeposit) {
			String depositAmountString = JOptionPane.showInputDialog(null, "Enter the number of chips you want to deposit:");
			try {
				int depositAmount = Integer.parseInt(depositAmountString);
				if (depositAmount >= 10) {
					playerChips += depositAmount;
					saveChipTotalToFile(); // Save the updated chip total to the file
					isValidDeposit = true; // Set flag to exit the loop
				} else {
					// Handle invalid deposit amount (less than 10)
					JOptionPane.showMessageDialog(null, "Invalid deposit amount. Please deposit at least 10 chips.", "Invalid Deposit", JOptionPane.ERROR_MESSAGE);
				}
			} catch (NumberFormatException e) {
				// Handle if the input is not a valid integer
				JOptionPane.showMessageDialog(null, "Invalid input. Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	//let the player decide if they want to deposit chips
	public void showDepositPanel() {
		int option = JOptionPane.showConfirmDialog(null, "You have 0 chips. Would you like to deposit more?", "Deposit Chips", JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			depositChips();  // call the resetChips() method to deposit chips
		} else {
			System.exit(0); // Terminate the program (you can adjust this based on your requirements)
		}
	}

	//build the deck of 52 cards
	public void buildDeck() {
		deck = new ArrayList<Card>();
		String[] values = { "Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King" };
		String[] types = { "Clubs", "Diamonds", "Hearts", "Spades" };

		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < values.length; j++) {
				Card card = new Card(values[j], types[i]);
				deck.add(card);
			}
		}
	}

	// method to shuffle the deck
	public void shuffleDeck() {
		for (int i = 0; i < deck.size(); i++) {
			int j = random.nextInt(deck.size());
			Card currCard = deck.get(i);
			Card randomCard = deck.get(j);
			deck.set(i, randomCard);
			deck.set(j, currCard);
		}
	}

	//get the user's input and set their bet for this hand
	public void setUserBet() {
		while (true) {
			String betFieldText = JOptionPane.showInputDialog(null, "Enter your bet (Remaing chips: " + playerChips + "):");
			try {
				if (betFieldText == null || betFieldText.trim().isEmpty()) {
					throw new NumberFormatException();
				}
				// Try to parse the input as an integer
				playerBet = Integer.parseInt(betFieldText);

				// Check if the parsed number is within the valid range
				if (playerBet < 10 || playerBet > 50000 || playerBet > playerChips) {
					throw new NumberFormatException();
				}
				// If everything is valid, break out of the loop
				break;
			} catch (NumberFormatException nfe) {
				// Handle if the input is empty, not a number, or outside the valid range
				JOptionPane.showMessageDialog(null, "Invalid Bet: Please enter a number between 10 and 50000 and it must be under " + playerChips,
						"Invalid Bet", JOptionPane.ERROR_MESSAGE);
			}
		}
		lowerPlayerChips(playerBet);
		System.out.println(currentPlayerUsername + " bet " + playerBet + " chips");
	}

	//lower the player's chips when the player loses
	public void lowerPlayerChips(int playerBet) {
		playerChips -= playerBet;
		saveChipTotalToFile(); // Save chip total to file after lowering
	}

	//raise the player's chips when the player wins
	public void raisePlayerChips(int playerBet, boolean isBlackjack, boolean isPush) {
		if(isPush) {
			playerChips += playerBet;
		}
		else if(isBlackjack) {
			playerChips += playerBet + ((playerBet * 3)/2);
		} else {
			playerChips += playerBet * 2;
		}
		saveChipTotalToFile(); // Save chip total to file after raising
	}

	// method to reduce the player's score if they go over 21 and have an ace in their hand
	public int reducePlayerAce() {
		while (playerHandSum > 21 && playerAceCount > 0) {
			playerHandSum -= 10;
			playerAceCount -= 1;
		}
		return playerHandSum;
	}

	// method to reduce the dealer's score if they go over 21 and have an ace in their hand
	public int reduceDealerAce() {
		while (dealerHandSum > 21 && dealerAceCount > 0) {
			dealerHandSum -= 10;
			dealerAceCount -= 1;
		}
		return dealerHandSum;
	}

	//get the string to show the user if they won or lost and how many chips were moved
	public String endGameText() {
		//both player and dealer have blackjack
		if(playerHandSum == 21 && dealerHandSum == 21){
			raisePlayerChips(playerBet, true, true);
			return  "Pushed! " + playerBet + " has been returned";
		} //player has blackjack and the dealer does not 
		else if(playerHandSum == 21 && dealerHandSum != 21) {
			raisePlayerChips(playerBet, true, false);
			return  "You have Blackjack! You won " + (playerBet + (playerBet*3)/2) + " chips";
		} //dealer has blackjack and the player does not 
		else if(dealerHandSum == 21 && playerHandSum != 21) {
			return  ("Dealer has Blackjack! You lost " + playerBet + " chips");
		} //player busts 
		else if(playerHandSum > 21) {
			return  "You busted! You lost " + playerBet + " chips";
		} //dealer busts 
		else if(dealerHandSum > 21) {
			raisePlayerChips(playerBet, false, false);
			return  "Dealer busted! You won " + playerBet + " chips";
		}//player and dealer have the same sum  
		else if(playerHandSum == dealerHandSum) {
			raisePlayerChips(playerBet, false, true);
			return  "Pushed! " + playerBet + " has been returned";
		} //player has a greater hand than the dealer
		else if(playerHandSum > dealerHandSum) {
			raisePlayerChips(playerBet, false, false);
			return  "You won " +  playerBet + " chips";
		} //dealer has a greater hand than the dealer
		else if(dealerHandSum > playerHandSum) {
			return  "You lost " + playerBet + " chips";
		}
		
		return null;
	}
}
